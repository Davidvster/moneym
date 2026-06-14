#!/usr/bin/env python3
"""
Compose Google Play store images from raw Paparazzi screenshots.

Two stages, kept separate so screenshots can be swapped without touching layout:
  1. build_template(...)  -> draws the near-black canvas, the title/subtitle text and
                             a code-drawn generic Android device frame (no camera cutout).
                             This is the reusable "wireframe" — independent of screenshot content.
  2. place_screenshot(...) -> drops a raw screenshot into the frame's screen rectangle.

Update flow when screenshots change:
  - re-record Paparazzi, copy the store_*.png into playstore/raw/  (see README.md)
  - run:  python3 playstore/frame_store.py
  No code edits needed — text + geometry live in config.json and this script.

Usage:
  python3 frame_store.py                 # render all framed images + hero into framed/
  python3 frame_store.py --templates     # also dump empty wireframes into templates/
"""

import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "raw")
FRAMED = os.path.join(HERE, "framed")
TEMPLATES = os.path.join(HERE, "templates")
ICON = os.path.join(HERE, "..", "androidApp", "src", "main", "ic_launcher-playstore.png")


# ── helpers ────────────────────────────────────────────────────────────────

def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def load_font(path, size):
    for p in (path, "/Library/Fonts/Arial.ttf", "/System/Library/Fonts/Supplemental/Arial.ttf"):
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()


def vertical_gradient(size, top_hex, bottom_hex):
    w, h = size
    top, bot = hex_rgb(top_hex), hex_rgb(bottom_hex)
    base = Image.new("RGB", (1, h))
    px = base.load()
    for y in range(h):
        t = y / max(1, h - 1)
        px[0, y] = tuple(round(top[i] + (bot[i] - top[i]) * t) for i in range(3))
    return base.resize((w, h))


def wrap(draw, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for word in words:
        trial = (cur + " " + word).strip()
        if draw.textlength(trial, font=font) <= max_w or not cur:
            cur = trial
        else:
            lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def draw_centered_block(draw, lines, font, color, cx, top, line_gap, leading=1.18):
    asc, desc = font.getmetrics()
    lh = round((asc + desc) * leading)
    y = top
    for ln in lines:
        w = draw.textlength(ln, font=font)
        draw.text((cx - w / 2, y), ln, font=font, fill=color)
        y += lh + line_gap
    return y


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


# ── device frame (code-drawn, generic Android, no camera cutout) ───────────

def make_device_frame(screen_w, screen_h, style, screenshot=None):
    """Return (frame RGBA, (sx, sy)) where sx,sy is the screen's top-left in the frame."""
    bezel = max(10, round(screen_w * 0.028))
    screen_radius = round(screen_w * 0.055)
    outer_radius = screen_radius + bezel
    fw, fh = screen_w + bezel * 2, screen_h + bezel * 2

    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    # subtle outer edge highlight then the black bezel body
    d.rounded_rectangle([0, 0, fw - 1, fh - 1], radius=outer_radius, fill=hex_rgb(style["bezel_edge"]))
    inset = max(2, round(bezel * 0.16))
    d.rounded_rectangle(
        [inset, inset, fw - 1 - inset, fh - 1 - inset],
        radius=outer_radius - inset, fill=hex_rgb(style["bezel_color"]),
    )

    # screen content (real screenshot or placeholder) with rounded corners
    if screenshot is None:
        screen = Image.new("RGB", (screen_w, screen_h), (32, 33, 36))
        sd = ImageDraw.Draw(screen)
        sd.line([(0, 0), (screen_w, screen_h)], fill=(70, 72, 76), width=3)
        sd.line([(screen_w, 0), (0, screen_h)], fill=(70, 72, 76), width=3)
        f = load_font(style["subtitle_font"], round(screen_w * 0.06))
        label = "SCREENSHOT"
        tw = sd.textlength(label, font=f)
        sd.text(((screen_w - tw) / 2, screen_h / 2 - screen_w * 0.04), label, font=f, fill=(120, 122, 126))
    else:
        screen = screenshot.convert("RGB").resize((screen_w, screen_h), Image.LANCZOS)
    frame.paste(screen, (bezel, bezel), rounded_mask((screen_w, screen_h), screen_radius))

    # side power button on the right edge
    btn_h = round(fh * 0.07)
    btn_y = round(fh * 0.20)
    d.rounded_rectangle(
        [fw - round(inset * 0.6), btn_y, fw + round(bezel * 0.30), btn_y + btn_h],
        radius=round(bezel * 0.25), fill=hex_rgb(style["bezel_edge"]),
    )
    return frame, bezel


# ── stage 1: template (canvas + text + frame), independent of screenshot ───

def build_template(device_cfg, style, title, subtitle, screenshot=None):
    cw, ch = device_cfg["canvas"]
    canvas = vertical_gradient((cw, ch), style.get("background_gradient", style["background"]),
                               style["background"]).convert("RGB")
    d = ImageDraw.Draw(canvas)

    title_font = load_font(style["title_font"], device_cfg["title_pt"])
    sub_font = load_font(style["subtitle_font"], device_cfg["subtitle_pt"])

    margin = round(cw * 0.08)
    top = round(ch * 0.045)
    title_lines = wrap(d, title, title_font, cw - margin * 2)
    y = draw_centered_block(d, title_lines, title_font, hex_rgb(style["title_color"]),
                            cw / 2, top, line_gap=round(cw * 0.004))
    # accent underline
    uw = round(cw * 0.10)
    d.rounded_rectangle([cw / 2 - uw / 2, y + round(ch * 0.006),
                         cw / 2 + uw / 2, y + round(ch * 0.006) + max(5, round(ch * 0.004))],
                        radius=6, fill=hex_rgb(style["accent"]))
    y += round(ch * 0.028)
    sub_lines = wrap(d, subtitle, sub_font, cw - margin * 2)
    y = draw_centered_block(d, sub_lines, sub_font, hex_rgb(style["subtitle_color"]),
                            cw / 2, y, line_gap=round(cw * 0.003))

    # device frame fills the area below the text
    area_top = y + round(ch * 0.03)
    area_bottom = ch - round(ch * 0.02)
    avail_h = area_bottom - area_top
    target_w = round(cw * device_cfg["device_width_frac"])

    if screenshot is not None:
        sw, sh = screenshot.size
    else:
        sw, sh = 1080, 2400
    aspect = sh / sw

    # size the *screen* so the whole frame (screen + bezel) fits both width and height
    screen_w = target_w
    bezel_est = round(screen_w * 0.028)
    screen_h = round(screen_w * aspect)
    if screen_h + bezel_est * 2 > avail_h:
        screen_h = avail_h - bezel_est * 2
        screen_w = round(screen_h / aspect)

    frame, bezel = make_device_frame(screen_w, screen_h, style, screenshot)
    fx = round((cw - frame.width) / 2)
    fy = area_top + round((avail_h - frame.height) / 2)
    canvas.paste(frame, (fx, fy), frame)
    return canvas


# ── hero (1024x500 landscape) ──────────────────────────────────────────────

def build_hero(cfg):
    style = cfg["style"]
    hero = cfg["hero"]
    # Render at SS× then downscale, so text / icon / frame edges stay crisp at the
    # small 1024×500 feature-graphic spec. All geometry below derives from w, h.
    ss = 3
    out_size = tuple(hero["size"])
    w, h = out_size[0] * ss, out_size[1] * ss
    canvas = vertical_gradient((w, h), style.get("background_gradient", style["background"]),
                               style["background"]).convert("RGB")
    d = ImageDraw.Draw(canvas)

    # right side: framed phone peeking up from the bottom
    raw_path = os.path.join(RAW, "store_phone_%s.png" % hero["screen"])
    shot = Image.open(raw_path) if os.path.exists(raw_path) else None
    if shot is not None:
        screen_h = round(h * 1.18)
        screen_w = round(screen_h * shot.size[0] / shot.size[1])
        frame, _ = make_device_frame(screen_w, screen_h, style, shot)
        scale = (h * 1.18) / frame.height
        frame = frame.resize((round(frame.width * scale), round(frame.height * scale)), Image.LANCZOS)
        canvas.paste(frame, (round(w * 0.66), round(h * 0.16)), frame)

    # left side: icon + wordmark + tagline
    pad = round(w * 0.06)
    cur_y = round(h * 0.20)
    if os.path.exists(ICON):
        icon = Image.open(ICON).convert("RGBA")
        isz = round(h * 0.20)
        icon = icon.resize((isz, isz), Image.LANCZOS)
        canvas.paste(icon, (pad, cur_y), rounded_mask((isz, isz), round(isz * 0.22)))
        title_x = pad + isz + round(w * 0.025)
        title_font = load_font(style["title_font"], round(h * 0.155))
        d.text((title_x, cur_y + round(isz * 0.16)), hero["title"], font=title_font,
               fill=hex_rgb(style["title_color"]))
        cur_y += isz + round(h * 0.06)
    else:
        title_font = load_font(style["title_font"], round(h * 0.16))
        d.text((pad, cur_y), hero["title"], font=title_font, fill=hex_rgb(style["title_color"]))
        cur_y += round(h * 0.22)

    sub_font = load_font(style["subtitle_font"], round(h * 0.052))
    max_text_w = round(w * 0.56)
    for ln in wrap(d, hero["subtitle"], sub_font, max_text_w):
        d.text((pad, cur_y), ln, font=sub_font, fill=hex_rgb(style["subtitle_color"]))
        cur_y += round(h * 0.072)

    cur_y += round(h * 0.02)
    feat_font = load_font(style["title_font"], round(h * 0.048))
    d.text((pad, cur_y), hero["feature"], font=feat_font, fill=hex_rgb(style["accent"]))

    canvas = canvas.resize(out_size, Image.LANCZOS)
    out = os.path.join(FRAMED, "hero_1024x500.png")
    canvas.save(out)
    print("hero  ->", os.path.relpath(out, HERE))


# ── driver ─────────────────────────────────────────────────────────────────

def main():
    dump_templates = "--templates" in sys.argv
    with open(os.path.join(HERE, "config.json")) as f:
        cfg = json.load(f)
    style = cfg["style"]
    os.makedirs(FRAMED, exist_ok=True)
    if dump_templates:
        os.makedirs(TEMPLATES, exist_ok=True)

    for device, dcfg in cfg["devices"].items():
        for slot in cfg["slots"]:
            screen = slot["screen"]
            raw_path = os.path.join(RAW, "store_%s_%s.png" % (device, screen))
            shot = Image.open(raw_path) if os.path.exists(raw_path) else None
            if shot is None:
                print("!! missing", os.path.relpath(raw_path, HERE), "- skipping")
                continue
            img = build_template(dcfg, style, slot["title"], slot["subtitle"], shot)
            out = os.path.join(FRAMED, "%s_%s.png" % (device, screen))
            img.save(out)
            print("frame ->", os.path.relpath(out, HERE), img.size)

            if dump_templates:
                tmpl = build_template(dcfg, style, slot["title"], slot["subtitle"], None)
                tout = os.path.join(TEMPLATES, "%s_%s.png" % (device, screen))
                tmpl.save(tout)

    build_hero(cfg)
    print("done.")


if __name__ == "__main__":
    main()
