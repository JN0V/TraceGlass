#!/usr/bin/env python3
"""Generate A4 PDF with 4 ArUco markers (4x4_50 dictionary, IDs 0-3) at corners.

Usage: python generate-markers.py [--margin 5] [--size 15] [--output aruco-markers-a4.pdf]
"""
import argparse
import numpy as np

try:
    import cv2
    from cv2 import aruco
except ImportError:
    print("pip install opencv-python-headless")
    raise

try:
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.units import mm
    from reportlab.pdfgen import canvas
    from reportlab.lib.utils import ImageReader
except ImportError:
    print("pip install reportlab")
    raise

from io import BytesIO
from PIL import Image


def generate_marker_image(marker_id: int, size_px: int = 200) -> Image.Image:
    dictionary = aruco.getPredefinedDictionary(aruco.DICT_4X4_50)
    marker = aruco.generateImageMarker(dictionary, marker_id, size_px)
    return Image.fromarray(marker)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--margin", type=float, default=5, help="Margin from edge in mm (default: 5)")
    parser.add_argument("--size", type=float, default=15, help="Marker size in mm (default: 15)")
    parser.add_argument("--output", default="aruco-markers-a4.pdf", help="Output PDF path")
    args = parser.parse_args()

    page_w, page_h = A4  # 210x297mm in points
    margin = args.margin * mm
    marker_size = args.size * mm

    c = canvas.Canvas(args.output, pagesize=A4)

    # Corners: TL=ID0, TR=ID1, BR=ID2, BL=ID3
    positions = [
        (margin, page_h - margin - marker_size),                    # TL (ID 0)
        (page_w - margin - marker_size, page_h - margin - marker_size),  # TR (ID 1)
        (page_w - margin - marker_size, margin),                    # BR (ID 2)
        (margin, margin),                                           # BL (ID 3)
    ]

    for marker_id, (x, y) in enumerate(positions):
        img = generate_marker_image(marker_id, size_px=300)
        buf = BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)
        c.drawImage(ImageReader(buf), x, y, width=marker_size, height=marker_size)

    c.save()
    print(f"Generated {args.output}: {args.size}mm markers, {args.margin}mm margin, A4")


if __name__ == "__main__":
    main()
