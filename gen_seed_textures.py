# -*- coding: utf-8 -*-
"""
DynamicTrees-EmeraldCraft 种子/果核物品纹理生成器

按树木种实的自然形态特征生成 16x16 的种子物品纹理，输出到
src/main/resources/assets/dynamictrees_emeraldcraft/textures/item/。

- 紫菌盖   purpuraceus_seed：近伞形，淡紫至深紫，边缘微内卷
- 银杏核   ginkgo_seed     ：长椭圆形，淡黄白色，表面纵棱
- 桃核     peach_seed      ：扁椭圆形，棕褐色，凹凸褶皱
- 棕榈果核 palm_seed       ：阔肾形，棕黑坚硬

运行方式：python gen_seed_textures.py
"""
import os
import math
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "src/main/resources/assets/dynamictrees_emeraldcraft/textures/item")


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))


def jitter(c, amount, rng):
    return tuple(clamp(v + rng.randint(-amount, amount)) for v in c) + (255,)


def put(px, x, y, c, rng, amount=3):
    px[x, y] = jitter(c, amount, rng)


def outline(px, shape, color, rng):
    """给形状 mask 加一圈深色描边（4 邻域）。"""
    border = set()
    for (x, y) in shape:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in shape and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                border.add(n)
    for (x, y) in border:
        px[x, y] = jitter(color, 6, rng)  # jitter 内部已补 alpha


def ell_rows(cx, cy, rx, ry, y_from, y_to):
    """返回 {y: (xmin, xmax)} 的实心椭圆行表。"""
    rows = {}
    for y in range(y_from, y_to + 1):
        dy = y - cy
        t = 1 - (dy / ry) ** 2
        if t <= 0:
            continue
        half = rx * math.sqrt(t)
        rows[y] = (int(round(cx - half)), int(round(cx + half)))
    return rows


# ------------------------------------------------------------- 1. 紫菌盖
def purpuraceus_seed():
    rng = random.Random(20260701)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    top = (206, 108, 186)     # 伞顶淡紫
    body = (172, 64, 150)     # 主体中紫
    rim = (124, 40, 112)      # 边缘内卷深紫
    stalk = (104, 52, 96)     # 菌柄
    stalk_dark = (80, 38, 74)
    hl = (228, 152, 208)      # 高光

    # 伞盖逐行宽度（近伞形：顶部圆拱、中下部最宽、底部收口内卷）
    cap_rows = {
        2: (6, 9),
        3: (5, 10),
        4: (4, 11),
        5: (3, 12),
        6: (2, 13),
        7: (2, 13),
        8: (3, 12),
        9: (3, 12),
        10: (4, 11),   # 内卷起点
        11: (5, 10),   # 内卷收口（描 rim 色）
    }
    shape = set()
    for y, (x0, x1) in cap_rows.items():
        for x in range(x0, x1 + 1):
            shape.add((x, y))
            # 颜色纵向渐变：上部淡紫 -> 下部深紫；最外圈 1px 为内卷 rim
            inner = (x0 < x < x1) and y < 10
            if y >= 10 or x in (x0, x1):
                base = rim
            elif y <= 4:
                base = top
            else:
                base = body
            put(px, x, y, base, rng, 4 if base is body else 3)
            shape_flag = True
    # 平滑高光：伞顶左上弧
    for (x, y) in ((6, 3), (7, 3), (5, 4), (6, 4), (7, 4), (5, 5)):
        if (x, y) in shape:
            put(px, x, y, hl, rng, 5)
    # 边缘内卷：y9-11 的下缘内侧加一圈亮紫（内卷受光面），强化「微微内卷」
    curl = (216, 144, 198)
    for y, xs in ((9, (3, 12)), (10, (4, 11)), (11, (5, 10))):
        for x in range(xs[0], xs[1] + 1):
            if (x, y) in shape and (y == 11 or x in (xs[0], xs[1])):
                put(px, x, y, curl, rng, 6)
    # 菌柄（短，居中）
    for y in (12, 13):
        for x in (7, 8):
            put(px, x, y, stalk if y == 12 else stalk_dark, rng, 4)
    outline(px, shape, (96, 28, 88), rng)
    return img


# ------------------------------------------------------------- 2. 银杏核
def ginkgo_seed():
    rng = random.Random(20260702)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    shell = (238, 228, 188)    # 淡黄白外壳
    ridge = (212, 198, 148)    # 纵棱（略深）
    groove = (222, 210, 164)   # 棱间浅槽
    shade = (214, 200, 152)    # 右下暗面
    hl = (250, 246, 218)       # 高光

    rows = ell_rows(8, 8, 4.6, 6.4, 1, 15)
    shape = set()
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            shape.add((x, y))
            # 三条纵棱（x=6/8/10 附近），棱是凸起所以亮一点，两侧槽略暗
            c = shell
            if x in (6, 9):
                c = ridge
            elif x in (5, 7, 8, 10):
                c = groove
            # 右下整体暗面
            if x > 9 or y > 11:
                c = shade if c is shell else c
            # 左上高光
            if (x, y) in ((6, 4), (7, 3), (7, 4), (6, 5), (8, 3)):
                c = hl
            put(px, x, y, c, rng, 3)
    outline(px, shape, (166, 148, 106), rng)
    return img


# ------------------------------------------------------------- 3. 桃核
def peach_seed():
    rng = random.Random(20260703)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = (152, 106, 62)      # 棕褐底
    deep = (118, 78, 42)       # 凹沟
    light = (180, 134, 88)     # 凸脊
    hl = (198, 156, 108)       # 高光

    rows = ell_rows(8, 8, 6.4, 5.4, 2, 15)
    shape = set()
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            shape.add((x, y))
            # 褶皱：伪随机沟脊交织（凹凸感）。用 (x*y+x^2) 的确定性哈希决定沟/脊
            h = (x * 7 + y * 13 + ((x * x + y * y) >> 1)) % 5
            if h == 0:
                c = deep
            elif h in (3, 4):
                c = light
            else:
                c = base
            # 边缘一圈暗
            if x in (x0, x1) or y in (min(rows), max(rows)):
                c = deep
            # 左上高光
            if (x, y) in ((5, 4), (6, 3), (6, 4), (5, 5), (7, 3)):
                c = hl
            put(px, x, y, c, rng, 4)
    outline(px, shape, (84, 54, 28), rng)
    return img


# ------------------------------------------------------------- 4. 棕榈果核
def palm_seed():
    rng = random.Random(20260704)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    shell = (74, 58, 46)       # 棕黑壳
    dark = (52, 40, 32)        # 暗部
    light = (100, 82, 66)      # 微高光
    hl = (116, 96, 78)         # 左上硬高光

    # 阔肾形（横放豆形）：顶部中央内凹（肾门），下部圆弧。手动行表保证凹口可读。
    notch = {3: (7, 8), 4: (7, 8), 5: (8, 8)}  # 每行中央挖去的 x 区间
    rows = {
        3: (5, 10),
        4: (3, 12),
        5: (2, 13),
        6: (2, 13),
        7: (2, 13),
        8: (3, 12),
        9: (4, 11),
        10: (5, 10),
        11: (6, 9),
    }
    shape = set()
    for y, (x0, x1) in rows.items():
        skip = notch.get(y)
        for x in range(x0, x1 + 1):
            if skip and skip[0] <= x <= skip[1]:
                continue
            shape.add((x, y))
            c = shell
            if x <= x0 + 1 or y >= 10:
                c = dark
            elif x >= x1 - 1:
                c = light
            # 肾门内凹边缘暗色
            if skip and x == skip[1] + 1:
                c = dark
            # 左上高光（坚硬光滑感）
            if (x, y) in ((5, 4), (6, 3), (6, 4), (4, 5), (5, 3)):
                c = hl
            put(px, x, y, c, rng, 3)
    outline(px, shape, (34, 26, 20), rng)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    purpuraceus_seed().save(os.path.join(OUT, "purpuraceus_seed.png"))
    print("written: purpuraceus_seed.png")
    ginkgo_seed().save(os.path.join(OUT, "ginkgo_seed.png"))
    print("written: ginkgo_seed.png")
    peach_seed().save(os.path.join(OUT, "peach_seed.png"))
    print("written: peach_seed.png")
    palm_seed().save(os.path.join(OUT, "palm_seed.png"))
    print("written: palm_seed.png")
    print("all done.")


if __name__ == "__main__":
    main()
