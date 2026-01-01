#!/usr/bin/env python3
"""
Golden Audiobook - Icon Generator Script

This script generates launcher icons in all required sizes from SVG source files.
It can output PNG or JPG format icons.

Usage:
    python icon_generator.py                    # Generate all sizes as PNG
    python icon_generator.py --format jpg       # Generate all sizes as JPG
    python icon_generator.py --size 512         # Generate only Play Store size
    python icon_generator.py --help             # Show help

Requirements:
    - Python 3.6+
    - CairoSVG (optional, for SVG to PNG conversion)
    - Or use: pip install cairosvg
"""

import os
import sys
import argparse
from pathlib import Path

# Try to import cairosvg, fall back to alternative methods
try:
    import cairosvg
    HAS_CAIRO = True
except ImportError:
    HAS_CAIRO = False
    print("Warning: CairoSVG not installed. Installing...")
    os.system("pip install cairosvg")
    try:
        import cairosvg
        HAS_CAIRO = True
    except:
        print("Error: CairoSVG installation failed. Please install manually:")
        print("  pip install cairosvg")
        sys.exit(1)


class IconGenerator:
    """Generator for Golden Audiobook app icons."""
    
    # App branding colors
    BACKGROUND_COLOR = "#FFC107"  # Golden Amber
    FOREGROUND_COLOR = "#FFFFFF"  # White
    
    # Required icon sizes for Android
    SIZES = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
        "playstore": 512,
    }
    
    def __init__(self, project_root=None):
        """Initialize the icon generator."""
        if project_root is None:
            self.project_root = Path(__file__).parent
        else:
            self.project_root = Path(project_root)
        
        self.res_dir = self.project_root / "app/src/main/res"
        self.drawable_dir = self.res_dir / "drawable"
        self.icon_guide_dir = self.project_root
        
    def generate_all_icons(self, output_format="png"):
        """Generate icons in all required sizes.
        
        Args:
            output_format: 'png' or 'jpg'
        """
        print("Golden Audiobook - Icon Generator")
        print("=" * 50)
        print(f"Output format: {output_format.upper()}")
        print(f"Project root: {self.project_root}")
        print()
        
        # Ensure output directories exist
        self._ensure_directories()
        
        # Generate icons for each size
        for density, size in self.SIZES.items():
            print(f"Generating {density} ({size}x{size})...")
            self._generate_icon(size, density, output_format)
            print(f"  ✓ Created {density}/ic_launcher.{output_format}")
            print(f"  ✓ Created {density}/ic_launcher_round.{output_format}")
        
        print()
        print("=" * 50)
        print("Icon generation complete!")
        print()
        print("Generated files:")
        for density in self.SIZES.keys():
            print(f"  - {density}/ic_launcher.{output_format}")
            print(f"  - {density}/ic_launcher_round.{output_format}")
    
    def _ensure_directories(self):
        """Ensure all mipmap directories exist."""
        for density in self.SIZES.keys():
            mipmap_dir = self.res_dir / f"mipmap-{density}"
            mipmap_dir.mkdir(parents=True, exist_ok=True)
    
    def _generate_icon(self, size, density, output_format):
        """Generate a single icon.
        
        Args:
            size: Size in pixels (square)
            density: Density name (e.g., 'hdpi')
            output_format: 'png' or 'jpg'
        """
        output_dir = self.res_dir / f"mipmap-{density}"
        svg_file = self.drawable_dir / "complete_icon.svg"
        
        if not svg_file.exists():
            raise FileNotFoundError(f"SVG source file not found: {svg_file}")
        
        output_path = output_dir / f"ic_launcher.{output_format}"
        output_path_round = output_dir / f"ic_launcher_round.{output_format}"
        
        # Convert SVG to image
        self._svg_to_image(svg_file, output_path, size, output_format)
        self._svg_to_image(svg_file, output_path_round, size, output_format)
    
    def _svg_to_image(self, svg_path, output_path, size, output_format):
        """Convert SVG to image using CairoSVG.
        
        Args:
            svg_path: Path to SVG file
            output_path: Path for output image
            size: Output size in pixels
            output_format: 'png' or 'jpg'
        """
        try:
            if output_format == "png":
                cairosvg.svg2png(
                    url=str(svg_path),
                    write_to=str(output_path),
                    output_width=size,
                    output_height=size
                )
            else:  # jpg
                # For JPG, we need to convert via PNG first
                temp_png = output_path.with_suffix(".png")
                cairosvg.svg2png(
                    url=str(svg_path),
                    write_to=str(temp_png),
                    output_width=size,
                    output_height=size
                )
                # Convert PNG to JPG
                from PIL import Image
                img = Image.open(temp_png)
                # Convert RGBA to RGB for JPG (add white background)
                if img.mode in ('RGBA', 'LA'):
                    background = Image.new('RGB', img.size, (255, 255, 255))
                    background.paste(img, mask=img.split()[-1])
                    img = background
                img.save(output_path, 'JPEG', quality=90)
                # Remove temp file
                temp_png.unlink()
                
        except Exception as e:
            print(f"Error converting {svg_path} to {output_path}: {e}")
            raise
    
    def generate_svg_only(self):
        """Generate and save standalone SVG files."""
        print("Generating standalone SVG files...")
        print()
        
        # Background SVG
        bg_svg = self.drawable_dir / "background.svg"
        bg_svg_content = self._get_background_svg()
        bg_svg.write_text(bg_svg_content)
        print(f"✓ Created {bg_svg}")
        
        # Foreground SVG
        fg_svg = self.drawable_dir / "foreground.svg"
        fg_svg_content = self._get_foreground_svg()
        fg_svg.write_text(fg_svg_content)
        print(f"✓ Created {fg_svg}")
        
        # Complete SVG
        complete_svg = self.drawable_dir / "complete_icon.svg"
        complete_svg_content = self._get_complete_svg()
        complete_svg.write_text(complete_svg_content)
        print(f"✓ Created {complete_svg}")
        
        print()
        print("SVG files ready for use!")
    
    def _get_background_svg(self):
        """Get background SVG content."""
        return f'''<?xml version="1.0" encoding="UTF-8"?>
<svg width="108" height="108" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
  <rect width="108" height="108" fill="#FFC107"/>
</svg>'''
    
    def _get_foreground_svg(self):
        """Get foreground SVG content."""
        return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="108" height="108" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
  <path d="M54 20C34.2 20 18 33.2 18 54c0 20.8 16.2 34 36 34 4 0 7.8 -0.7 11.4 -1.9
           c-2.7 -1.6 -5.2 -3.8 -7.4 -6.6 -5.8 3.3 -12.8 5.2 -20.6 5.2 -12.2 0 -23.1 -4.9 -30.7 -12.9
           C23.7 65.1 28.8 59 35.8 55.2c-1.1 -2.7 -1.7 -5.7 -1.7 -8.8C34.1 29.5 39.5 24 46.8 24L54 20z"
        fill="#FFFFFF"/>
  <path d="M54 20l7.2 4c7.3 0 12.7 5.5 12.7 12.4 0 3.1 -0.6 6.1 -1.7 8.8
           7 3.8 12.1 9.9 14.7 17.1 7.6 8 18.5 12.9 30.7 12.9 7.8 0 14.8 -1.9 20.6 -5.2
           2.2 2.8 4.7 5 7.4 6.6 -3.6 1.2 -7.4 1.9 -11.4 1.9C73.8 88 90 74.8 90 54
           C90 33.2 73.8 20 54 20z"
        fill="#F5F5F5"/>
  <path d="M54 20v68c4 0 7.8 -0.7 11.4 -1.9V21.9C61.8 21.2 58 20 54 20z"
        fill="#E8E8E8"/>
  <path d="M47 42L69 54L47 66z" fill="#FFC107"/>
  <path d="M47 42L69 54L47 54z" fill="#FFD54F"/>
</svg>'''
    
    def _get_complete_svg(self):
        """Get complete SVG content."""
        return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="108" height="108" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
  <rect width="108" height="108" fill="#FFC107"/>
  <g transform="translate(54, 54) scale(0.55) translate(-54, -54)">
    <path d="M54 20C34.2 20 18 33.2 18 54c0 20.8 16.2 34 36 34 4 0 7.8 -0.7 11.4 -1.9
             c-2.7 -1.6 -5.2 -3.8 -7.4 -6.6 -5.8 3.3 -12.8 5.2 -20.6 5.2 -12.2 0 -23.1 -4.9 -30.7 -12.9
             C23.7 65.1 28.8 59 35.8 55.2c-1.1 -2.7 -1.7 -5.7 -1.7 -8.8C34.1 29.5 39.5 24 46.8 24L54 20z"
          fill="#FFFFFF"/>
    <path d="M54 20l7.2 4c7.3 0 12.7 5.5 12.7 12.4 0 3.1 -0.6 6.1 -1.7 8.8
             7 3.8 12.1 9.9 14.7 17.1 7.6 8 18.5 12.9 30.7 12.9 7.8 0 14.8 -1.9 20.6 -5.2
             2.2 2.8 4.7 5 7.4 6.6 -3.6 1.2 -7.4 1.9 -11.4 1.9C73.8 88 90 74.8 90 54
             C90 33.2 73.8 20 54 20z"
          fill="#F5F5F5"/>
    <path d="M54 20v68c4 0 7.8 -0.7 11.4 -1.9V21.9C61.8 21.2 58 20 54 20z"
          fill="#E8E8E8"/>
    <path d="M47 42L69 54L47 66z" fill="#FFC107"/>
    <path d="M47 42L69 54L47 54z" fill="#FFD54F"/>
  </g>
</svg>'''


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Golden Audiobook - Icon Generator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python icon_generator.py                    # Generate all PNG icons
  python icon_generator.py --format jpg       # Generate all JPG icons
  python icon_generator.py --svg-only         # Generate SVG files only
  python icon_generator.py --help             # Show this help

For more information, see ICON_GENERATION_GUIDE.md
        """
    )
    
    parser.add_argument(
        "--format", "-f",
        choices=["png", "jpg"],
        default="png",
        help="Output format (default: png)"
    )
    
    parser.add_argument(
        "--size", "-s",
        choices=list(IconGenerator.SIZES.keys()),
        help="Generate only specific size"
    )
    
    parser.add_argument(
        "--svg-only",
        action="store_true",
        help="Generate SVG files only (no PNG/JPG conversion)"
    )
    
    parser.add_argument(
        "--project-root", "-p",
        help="Path to project root directory"
    )
    
    args = parser.parse_args()
    
    # Create generator
    generator = IconGenerator(args.project_root)
    
    if args.svg_only:
        generator.generate_svg_only()
    else:
        generator.generate_all_icons(args.format)


if __name__ == "__main__":
    main()
