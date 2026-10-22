import json
import math
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
JAR = Path(__import__("sys").argv[1]) if len(__import__("sys").argv) > 1 else ROOT.parent / "26.2-0.20.1.jar"
WORTH = ROOT / "src/main/resources/assets/materialism/worth.json"
CATALOG = ROOT / "src/main/resources/assets/materialism/shop.json"
CONFIG = ROOT / "run/config/materialism-shop.json"
RECIPE_PREFIX = "data/minecraft/recipe/"
TAG_PREFIX = "data/minecraft/tags/item/"

values = json.load(open(WORTH, encoding="utf-8"))["items"]
catalog = json.load(open(CATALOG, encoding="utf-8"))
config = json.load(open(CONFIG, encoding="utf-8")) if CONFIG.exists() else {}
multiplier = float(config.get("buyPriceMultiplier", 3.0))
overrides = config.get("itemPriceOverrides", {}) or {}
box_override = config.get("shulkerBoxPriceOverride")
shop = {entry["id"]: entry for entry in catalog["items"]}
errors = []
warnings = []


def units(value):
    return max(1, round(value * 10000))


def buy_units(item_id):
    if item_id in overrides:
        return max(1, round(float(overrides[item_id]) * 10000))
    return max(1, math.ceil(units(values[item_id]) * multiplier))


def box_price():
    if box_override is not None:
        return max(1, round(float(box_override) * 10000))
    return buy_units("minecraft:shulker_box")


def sell_units(item_id):
    return units(values[item_id])


excluded_exact = {
    "minecraft:netherite_scrap",
    "minecraft:netherite_ingot",
    "minecraft:netherite_block",
    "minecraft:ancient_debris",
    "minecraft:netherite_upgrade_smithing_template",
    "minecraft:elytra",
    "minecraft:totem_of_undying",
    "minecraft:nether_star",
    "minecraft:beacon",
    "minecraft:heavy_core",
    "minecraft:mace",
    "minecraft:dragon_egg",
    "minecraft:dragon_head",
    "minecraft:enchanted_golden_apple",
    "minecraft:wither_skeleton_skull",
    "minecraft:conduit",
}
excluded_patterns = [
    r"^minecraft:netherite_",
    r"^minecraft:.*_spawn_egg$",
    r"^minecraft:.*_(sword|pickaxe|axe|shovel|hoe|spear)$",
    r"^minecraft:.*_(helmet|chestplate|leggings|boots)$",
    r"^minecraft:(bow|crossbow|shield|arrow|spectral_arrow|fishing_rod)$",
    r"_(head|skull)$",
    r"_(horse_armor|nautilus_armor|harness)$",
    r"_(banner_pattern|pottery_sherd)$",
    r"^minecraft:music_disc_",
]

for item_id in shop:
    if item_id not in values:
        errors.append(f"catalog item missing from worth.json: {item_id}")
        continue
    if values[item_id] <= 0:
        errors.append(f"catalog item has non-positive sell value: {item_id}")
    if buy_units(item_id) <= sell_units(item_id):
        errors.append(f"buy price is not above sell price: {item_id}")
    if item_id in excluded_exact or any(re.search(p, item_id) for p in excluded_patterns):
        errors.append(f"excluded item is in shop: {item_id}")

for required in ("minecraft:shulker_box", "minecraft:shulker_shell", "minecraft:trident"):
    if required not in shop:
        errors.append(f"required item missing from shop: {required}")
if "minecraft:wither_skeleton_skull" in shop:
    errors.append("wither skeleton skull must not be in shop")

for item_id in shop:
    if item_id.endswith("_ore") or item_id == "minecraft:amethyst_cluster":
        errors.append(f"Fortune-sensitive block is in shop: {item_id}")

# Container integrity: bought box + full contents must always resell at a loss.
box_buy = box_price()
box_sell = sell_units("minecraft:shulker_box")
for item_id in shop:
    if item_id.endswith("_shulker_box"):
        continue
    capacity = 27 * 64
    content_buy = buy_units(item_id) * capacity
    content_sell = sell_units(item_id) * capacity
    if content_sell + box_sell > content_buy + box_buy:
        errors.append(f"full shulker packaging is profitable: {item_id}")

with zipfile.ZipFile(JAR) as zf:
    names = set(zf.namelist())
    tags = {}
    recipes = []
    for name in names:
        if name.startswith(TAG_PREFIX) and name.endswith(".json"):
            tag_id = "minecraft:" + name[len(TAG_PREFIX):-5]
            data = json.loads(zf.read(name))
            tags[tag_id] = data.get("values", []) if isinstance(data, dict) else data
        if name.startswith(RECIPE_PREFIX) and name.endswith(".json"):
            recipe = json.loads(zf.read(name))
            recipe["_id"] = "minecraft:" + name[len(RECIPE_PREFIX):-5]
            recipes.append(recipe)


def tag_members(tag_id, seen=None):
    seen = set() if seen is None else seen
    if tag_id in seen or tag_id not in tags:
        return []
    seen.add(tag_id)
    out = []
    for value in tags[tag_id]:
        if isinstance(value, str) and value.startswith("#"):
            out.extend(tag_members(value[1:], seen))
        elif isinstance(value, str):
            out.append(value)
        elif isinstance(value, dict) and value.get("required", True):
            ref = value.get("id")
            if isinstance(ref, str):
                if ref.startswith("#"):
                    out.extend(tag_members(ref[1:], seen))
                else:
                    out.append(ref)
    return out


cache = {}


def buy_cost(ingredient):
    if isinstance(ingredient, str):
        if ingredient.startswith("#"):
            key = ingredient[1:]
            cache.setdefault(key, tag_members(key))
            known = [buy_units(item) for item in cache[key] if item in shop]
            return min(known) if known else None
        return buy_units(ingredient) if ingredient in shop else None
    if isinstance(ingredient, list):
        known = [cost for entry in ingredient if (cost := buy_cost(entry)) is not None]
        return min(known) if known else None
    if isinstance(ingredient, dict):
        for key in ("item", "id"):
            if key in ingredient:
                return buy_cost(ingredient[key])
        if "tag" in ingredient:
            return buy_cost("#" + ingredient["tag"])
        if "items" in ingredient:
            return buy_cost(ingredient["items"])
    return None


def recipe_groups(recipe):
    kind = recipe.get("type")
    if kind == "minecraft:crafting_shaped":
        pattern = recipe.get("pattern", [])
        key = recipe.get("key", {})
        return [(key[s], sum(row.count(s) for row in pattern)) for s in set("".join(pattern)) if s != " " and s in key], 1.0
    if kind == "minecraft:crafting_shapeless":
        return [(i, 1) for i in recipe.get("ingredients", [])], 1.0
    if kind in {"minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting"}:
        return [(recipe.get("ingredient"), 1)], 1.05
    if kind == "minecraft:smithing_transform":
        return [(recipe.get(k), 1) for k in ("template", "base", "addition")], 1.0
    if kind == "minecraft:crafting_transmute":
        return [(recipe.get("input"), 1), (recipe.get("material"), 1)], 1.0
    if kind == "minecraft:crafting_imbue":
        return [(recipe.get("material"), 1), (recipe.get("source"), 1)], 1.0
    if kind == "minecraft:crafting_decorated_pot":
        return [(recipe.get(k), 1) for k in ("front", "back", "left", "right")], 1.0
    if kind == "minecraft:crafting_special_firework_star":
        return [(recipe.get("dye"), 1), (recipe.get("fuel"), 1)], 1.0
    return None, 1.0


loops = []
for recipe in recipes:
    result = recipe.get("result", {})
    if not result:
        continue
    if isinstance(result, str):
        result = {"id": result}
    out = result.get("id")
    count = result.get("count", 1)
    if out not in values:
        continue
    groups, bonus = recipe_groups(recipe)
    if groups is None:
        continue
    total = 0
    unknown = False
    for ingredient, quantity in groups:
        cost = buy_cost(ingredient)
        if cost is None:
            unknown = True
            break
        total += cost * quantity
    if unknown:
        continue
    total *= bonus
    profit = sell_units(out) * count - total
    if profit > 1:
        loops.append((profit, recipe["_id"], out, count, total))

if loops:
    loops.sort(reverse=True)
    for profit, recipe_id, out, count, total in loops[:50]:
        errors.append(f"buy/craft/sell loop: {recipe_id} -> {out} x{count}, profit {profit / 10000:.4f}c")

print(f"shop items: {len(shop)}")
print(f"multiplier: {multiplier}")
print(f"shulker box price: {box_buy / 10000:.2f}c")
print(f"shulker box resale: {box_sell / 10000:.2f}c")
print(f"recipe loops: {len(loops)}")
if errors:
    print(f"FAILED ({len(errors)} errors)")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)
print("OK")
