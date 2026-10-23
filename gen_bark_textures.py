# -*- coding: utf-8 -*-
"""
DynamicTrees-EmeraldCraft 树皮纹理生成器

使用程序化算法生成 16x16 的树皮/年轮/剥皮原木纹理，输出到
src/main/resources/assets/dynamictrees_emeraldcraft/textures/block/。

运行方式：python gen_bark_textures.py
"""
import os
import math
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "src/main/resources/assets/dynamictrees_emeraldcraft/textures/block")

# ---------------------------------------------------------------- utilities

def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))

def jitter(base, amount, rng):
    return tuple(clamp(c + rng.randint(-amount, amount)) for c in base)

def put(px, x, y, c, rng, amount=3):
    px[x, y] = jitter(c, amount, rng) + (255,)

def save(img, name):
    img.save(os.path.join(OUT, name))
    print("written:", name)

# ------------------------------------------------------- 1. ginkgo 银杏树皮
# 风格：浅灰褐底 + 竖向裂纹条带（参考真实银杏老树皮的浅色竖纹）。
def ginkgo_bark():
    rng = random.Random(20260601)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    highlight = (196, 186, 158)  # 浅条带（受光竖脊）
    base = (172, 162, 134)       # 浅灰褐底
    mid = (148, 138, 112)        # 中间调竖纹
    groove = (122, 110, 88)      # 竖向裂纹沟

    # 竖条带：每列按确定性噪声决定基准亮度
    band = [rng.random() for _ in range(16)]
    for x in range(16):
        for y in range(16):
            b = band[x] * 0.7 + band[(x + 1) % 16] * 0.3  # 相邻列混合，柔化
            if b > 0.72:
                c = highlight
            elif b > 0.42:
                c = base
            else:
                c = mid
            px[x, y] = jitter(c, 4, rng) + (255,)

    # 三条竖向裂纹沟：每 3~5 行随机左右偏移 1px，制造自然弯曲
    for gx in (3, 8, 13):
        x = gx
        for y in range(16):
            put(px, x, y, groove, rng, 4)
            # 沟的一侧带一点阴影，增强凹陷感
            if x + 1 < 16:
                put(px, x + 1, y, mid, rng, 4)
            if rng.random() < 0.3:
                x = max(0, min(15, x + rng.choice((-1, 1))))
    return img

def ginkgo_rings():
    # 年轮：浅灰褐底 + 同心偏环（与浅色树皮协调）
    rng = random.Random(20260602)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    light = (188, 178, 148)
    dark = (150, 140, 112)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            band = math.sin(d * 2.1) * 0.5 + 0.5
            c = dark if band > 0.5 else light
            px[x, y] = jitter(c, 7, rng) + (255,)
    return img

# ------------------------------------------------------- 2. palm 棕榈树皮
# 风格：横向环状叶痕带（palm 天然特征）。EC 棕榈为杂乱噪点，此处改为规则环带。
def palm_bark():
    rng = random.Random(20260603)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    band_light = (104, 88, 60)
    band_dark = (72, 58, 40)
    seam = (52, 42, 30)
    for y in range(16):
        # 每 4px 一个环带周期：亮带 2px + 暗带 2px，接缝 1px
        phase = y % 4
        for x in range(16):
            if phase == 0:
                c = seam
            elif phase in (1, 2):
                c = band_light
            else:
                c = band_dark
            # 环带内加少量纵向纤维抖动
            if phase in (1, 2) and rng.random() < 0.15:
                c = band_dark
            px[x, y] = jitter(c, 5, rng) + (255,)
    return img

def palm_rings():
    rng = random.Random(20260604)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    light = (122, 104, 72)
    dark = (94, 78, 52)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            band = math.sin(d * 1.7) * 0.5 + 0.5
            c = dark if band > 0.55 else light
            px[x, y] = jitter(c, 7, rng) + (255,)
    return img

# ------------------------------------------------------- 3. peach 桃树皮
# 风格：光滑亮底 + 横向皮孔（lenticel，樱桃/桃树真实特征）。
# EC 桃树为浅色竖纹，此处改为光滑横底 + 深色短横孔。
def peach_bark():
    rng = random.Random(20260605)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    smooth = (150, 128, 92)     # 光滑底（暖灰褐）
    smooth2 = (138, 116, 82)    # 微暗底
    lenticel = (96, 76, 50)     # 皮孔（深色短横条）
    for y in range(16):
        for x in range(16):
            c = smooth if (x + y) % 3 else smooth2
            px[x, y] = jitter(c, 4, rng) + (255,)
    # 撒皮孔：短横条 2~3px
    for _ in range(14):
        lx = rng.randint(0, 13)
        ly = rng.randint(0, 15)
        ln = rng.choice((2, 3))
        for dx in range(ln):
            px[(lx + dx) % 16, ly] = jitter(lenticel, 5, rng) + (255,)
    return img

def peach_rings():
    rng = random.Random(20260606)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    light = (168, 148, 108)
    dark = (128, 106, 72)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            band = math.sin(d * 1.9 + 0.8) * 0.5 + 0.5
            c = dark if band > 0.5 else light
            px[x, y] = jitter(c, 7, rng) + (255,)
    return img

# ------------------------------------------------------- 4. purpuraceus 紫菌木
# 风格：下界菌茎——紫底 + 亮紫疣点（wart），横向波纹。EC 菌茎为暗紫斜纹。
def purpuraceus_bark():
    rng = random.Random(20260607)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    stem = (108, 44, 84)      # 菌茎紫
    stem_dark = (84, 32, 64)  # 暗紫沟
    wart = (168, 92, 140)     # 亮紫疣点
    for y in range(16):
        wave = math.sin(y * math.pi / 4.0)
        for x in range(16):
            t = math.sin((x + wave * 2) * math.pi / 5.0)
            c = stem_dark if t > 0.45 else stem
            px[x, y] = jitter(c, 6, rng) + (255,)
    # 撒亮疣点
    for _ in range(18):
        wx = rng.randint(0, 15)
        wy = rng.randint(0, 15)
        px[wx, wy] = jitter(wart, 8, rng) + (255,)
        if rng.random() < 0.5:  # 部分疣点带一个邻居
            px[(wx + 1) % 16, wy] = jitter(wart, 10, rng) + (255,)
    return img

def purpuraceus_rings():
    rng = random.Random(20260608)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    light = (140, 66, 108)
    dark = (96, 40, 74)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            band = math.sin(d * 2.3) * 0.5 + 0.5
            c = dark if band > 0.5 else light
            px[x, y] = jitter(c, 8, rng) + (255,)
    return img

# ------------------------------------------------------- 5. purpuraceus 菌盖
# 风格：菌盖面——紫底 + 同心斑点环（区别于 EC 的 wart block 噪点）。
def purpuraceus_cap():
    rng = random.Random(20260609)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    cap = (128, 62, 108)
    cap_dark = (100, 44, 84)
    spot = (196, 128, 176)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = cap_dark if int(d) % 3 == 0 else cap
            px[x, y] = jitter(c, 6, rng) + (255,)
    for _ in range(10):
        sx = rng.randint(0, 15)
        sy = rng.randint(0, 15)
        px[sx, sy] = jitter(spot, 8, rng) + (255,)
    return img

# ------------------------------------------------------- stripped 变体
# 剥皮原木：去掉树皮图案，统一为纵向浅色木质 + 平行细纹。
def stripped(base_light, base_dark, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for x in range(16):
        # 每列一种纵向纹理亮度（木板条纹感）
        col_bias = rng.uniform(-1, 1)
        for y in range(16):
            c = base_dark if col_bias > 0.45 else base_light
            px[x, y] = jitter(c, 4, rng) + (255,)
    return img

def main():
    os.makedirs(OUT, exist_ok=True)
    # bark + rings
    save(ginkgo_bark(), "ginkgo_bark.png")
    save(ginkgo_rings(), "ginkgo_rings.png")
    save(palm_bark(), "palm_bark.png")
    save(palm_rings(), "palm_rings.png")
    save(peach_bark(), "peach_bark.png")
    save(peach_rings(), "peach_rings.png")
    save(purpuraceus_bark(), "purpuraceus_bark.png")
    save(purpuraceus_rings(), "purpuraceus_rings.png")
    save(purpuraceus_cap(), "purpuraceus_cap.png")
    # stripped: 各自的浅色木质
    save(stripped((176, 168, 108), (150, 142, 88), 20260611), "stripped_ginkgo_bark.png")
    save(stripped((196, 186, 136), (170, 160, 112), 20260612), "stripped_ginkgo_rings.png")
    save(stripped((164, 142, 104), (138, 118, 84), 20260613), "stripped_palm_bark.png")
    save(stripped((186, 164, 124), (160, 140, 104), 20260614), "stripped_palm_rings.png")
    save(stripped((188, 160, 116), (162, 136, 96), 20260615), "stripped_peach_bark.png")
    save(stripped((206, 180, 136), (178, 152, 112), 20260616), "stripped_peach_rings.png")
    save(stripped((176, 96, 140), (146, 76, 114), 20260617), "stripped_purpuraceus_bark.png")
    save(stripped((196, 118, 158), (166, 96, 132), 20260618), "stripped_purpuraceus_rings.png")
    print("all done.")

if __name__ == "__main__":
    main()
