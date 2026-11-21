import os
import math
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

def make_spacerust_logo():
    src_path = r'C:/Users/Dimasta/.gemini/antigravity/brain/aefb9dd5-694b-448f-8d4d-752be0361032/.user_uploaded/media_1795295811157_92873661.png'
    src = Image.open(src_path).convert('RGBA')
    
    W, H = 512, 512
    src_512 = src.resize((W, H), Image.Resampling.LANCZOS)
    arr = np.array(src_512)
    
    # Emblem mask: extract exact geometry
    r = arr[:, :, 0].astype(float)
    g = arr[:, :, 1].astype(float)
    b = arr[:, :, 2].astype(float)
    
    emblem_mask = ((r < 110) & (g < 70) & (b < 70)).astype(np.uint8) * 255
    emblem_mask_img = Image.fromarray(emblem_mask, mode='L')
    emblem_mask_smooth = emblem_mask_img.filter(ImageFilter.GaussianBlur(0.8))

    # 1. Vibrant Rust Background with Cosmic Vignette
    bg = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    cx, cy = W // 2, H // 2
    
    # Generate high quality radial gradient
    y_coords, x_coords = np.ogrid[:H, :W]
    dist_from_center = np.sqrt((x_coords - cx)**2 + (y_coords - cy)**2) / (W * 0.5)
    
    # Center: vibrant Rust red [215, 68, 40]
    # Edge: deep dark rust [120, 25, 15]
    # Corners: space dark [30, 10, 8]
    t = np.clip(dist_from_center, 0.0, 1.4)
    bg_r = np.clip(225 - t * 135, 20, 230).astype(np.uint8)
    bg_g = np.clip(72 - t * 50, 8, 80).astype(np.uint8)
    bg_b = np.clip(45 - t * 34, 6, 50).astype(np.uint8)
    bg_a = np.full((H, W), 255, dtype=np.uint8)
    
    bg_arr = np.stack([bg_r, bg_g, bg_b, bg_a], axis=2)
    bg = Image.fromarray(bg_arr, mode='RGBA')

    # 2. Add fine cosmic stars
    np.random.seed(1337)
    star_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    star_draw = ImageDraw.Draw(star_layer)
    for _ in range(70):
        sx = int(np.random.randint(15, W - 15))
        sy = int(np.random.randint(15, H - 15))
        alpha = int(np.random.randint(70, 220))
        size = 1 if np.random.rand() > 0.15 else 2
        star_draw.ellipse([sx, sy, sx + size, sy + size], fill=(255, 245, 230, alpha))
    bg = Image.alpha_composite(bg, star_layer)

    # 3. Add glowing orbital cosmic ring (BEHIND the emblem)
    ring_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ring_draw = ImageDraw.Draw(ring_layer)
    rx, ry = 195, 68
    angle_deg = -22
    rot_rad = math.radians(angle_deg)
    
    ring_pts = []
    for deg in range(0, 360, 2):
        rad = math.radians(deg)
        x0 = rx * math.cos(rad)
        y0 = ry * math.sin(rad)
        xr = x0 * math.cos(rot_rad) - y0 * math.sin(rot_rad) + cx
        yr = x0 * math.sin(rot_rad) + y0 * math.cos(rot_rad) + cy
        ring_pts.append((xr, yr))

    # Outer glow of ring
    for i in range(len(ring_pts)):
        p1 = ring_pts[i]
        p2 = ring_pts[(i + 1) % len(ring_pts)]
        ring_draw.line([p1, p2], fill=(255, 170, 70, 180), width=6)
    
    ring_glow = ring_layer.filter(ImageFilter.GaussianBlur(5))
    ring_layer = Image.alpha_composite(ring_glow, ring_layer)

    # Core bright line of ring
    ring_core = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(ring_core)
    for i in range(len(ring_pts)):
        p1 = ring_pts[i]
        p2 = ring_pts[(i + 1) % len(ring_pts)]
        c_draw.line([p1, p2], fill=(255, 235, 190, 255), width=2)
    ring_layer = Image.alpha_composite(ring_layer, ring_core)

    # Composite ring into bg (so it sits naturally behind the emblem!)
    bg = Image.alpha_composite(bg, ring_layer)

    # 4. Shadow under emblem for dramatic 3D depth
    shadow_mask = emblem_mask_smooth.filter(ImageFilter.GaussianBlur(8))
    shadow_layer = Image.new('RGBA', (W, H), (10, 2, 2, 190))
    bg.paste(shadow_layer, (0, 5), mask=shadow_mask)

    # 5. Glowing edge / aura for the emblem
    aura_mask = emblem_mask_smooth.filter(ImageFilter.MaxFilter(5))
    aura_diff = Image.fromarray(np.clip(np.array(aura_mask) - np.array(emblem_mask_img), 0, 255))
    aura_glow = Image.new('RGBA', (W, H), (255, 130, 45, 160))
    bg.paste(aura_glow, (0, 0), mask=aura_diff.filter(ImageFilter.GaussianBlur(2)))

    # 6. Emblem: sleek deep brutalist charcoal with subtle texture
    emblem_col = Image.new('RGBA', (W, H), (26, 20, 22, 255))
    bg.paste(emblem_col, (0, 0), mask=emblem_mask_smooth)

    # 7. Add a subtle cosmic star flare on the central diamond
    flare_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    f_draw = ImageDraw.Draw(flare_layer)
    fx, fy = cx, cy
    # Draw cross flare
    flare_len = 16
    f_draw.line([fx - flare_len, fy, fx + flare_len, fy], fill=(255, 230, 180, 220), width=2)
    f_draw.line([fx, fy - flare_len, fx, fy + flare_len], fill=(255, 230, 180, 220), width=2)
    f_draw.ellipse([fx - 3, fy - 3, fx + 3, fy + 3], fill=(255, 255, 255, 255))
    flare_blur = flare_layer.filter(ImageFilter.GaussianBlur(1.5))
    bg = Image.alpha_composite(bg, Image.alpha_composite(flare_blur, flare_layer))

    # 8. Rounded Squircle Mask (Modern Windows Game Launcher Icon)
    squircle_mask = Image.new('L', (W, H), 0)
    s_draw = ImageDraw.Draw(squircle_mask)
    corner_radius = 82
    margin = 8
    s_draw.rounded_rectangle([margin, margin, W - margin, H - margin], radius=corner_radius, fill=255)

    final_icon = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    final_icon.paste(bg, (0, 0), mask=squircle_mask)

    # Crisp metallic bevel border
    border_layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(border_layer)
    b_draw.rounded_rectangle([margin, margin, W - margin, H - margin], radius=corner_radius, outline=(255, 140, 70, 220), width=3)
    b_draw.rounded_rectangle([margin+1, margin+1, W - margin-1, H - margin-1], radius=corner_radius-1, outline=(40, 12, 10, 160), width=2)
    final_icon = Image.alpha_composite(final_icon, border_layer)

    # Save to workspace & brain
    out_dir = r'C:\Users\Dimasta\Desktop\spacerust'
    brain_dir = r'C:\Users\Dimasta\.gemini\antigravity\brain\aefb9dd5-694b-448f-8d4d-752be0361032'

    png_path = os.path.join(out_dir, 'spacerust_icon.png')
    ico_path = os.path.join(out_dir, 'spacerust_icon.ico')
    final_icon.save(png_path, 'PNG')
    final_icon.save(ico_path, format='ICO', sizes=[(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)])

    # Also save to assets directory for web launcher
    assets_dir = os.path.join(out_dir, 'assets')
    os.makedirs(assets_dir, exist_ok=True)
    final_icon.save(os.path.join(assets_dir, 'logo.png'), 'PNG')
    final_icon.save(os.path.join(assets_dir, 'favicon.ico'), format='ICO', sizes=[(64, 64), (32, 32), (16, 16)])

    # Copy to brain
    final_icon.save(os.path.join(brain_dir, 'spacerust_icon.png'), 'PNG')

    print("Success! Generated all icon sizes and formats.")

if __name__ == '__main__':
    make_spacerust_logo()
