void setup() {
  size(400, 400);
  File folder = new File(sketchPath());
  for (File file : folder.listFiles()) {
    String fileName = file.getName();
    if (fileName.endsWith(".png")) {
      println(file.getName());
      fixImage(file.getName());
    }
  }
}

void fixImage(String file) {
  // Flatten PNG images so that all they contain is alpha transparency,
  // the RGB values are always white, no darkened grayscale that could obscure
  // pixels behind it when we simulate LEDs or their sparkle
  PImage img = loadImage(file);
  img.loadPixels();
  for (int i = 0; i < img.pixels.length; ++i) {
    int pixel = img.pixels[i];
    int r = (pixel >> 16) & 0xff;
    int g = (pixel >> 8) & 0xff;
    int b = (pixel) & 0xff;
    int alpha = (pixel >> 24) & 0xff;
    int max = (int) Math.round(alpha / 255. * max(r, g, b));    
    img.pixels[i] = (max << 24) | 0x00ffffff;
  }
  img.updatePixels();
  img.save("fix/" + file);
  
  exit();
}
