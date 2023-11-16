package fun.fan.xc.starter.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageTools {
    /**
     * 图片拼接
     *
     * @param images 图片数组
     * @param type   1 横向拼接， 2 纵向拼接
     *               （注意：必须两张图片长宽一致）
     */
    public static ByteArrayOutputStream mergeImage(BufferedImage[] images, int type) throws IOException {
        int len = images.length;
        int[][] ImageArrays = new int[len][];

        for (int i = 0; i < len; i++) {
            int width = images[i].getWidth();
            int height = images[i].getHeight();
            ImageArrays[i] = new int[width * height];
            ImageArrays[i] = images[i].getRGB(0, 0, width, height, ImageArrays[i], 0, width);
        }
        int newHeight = 0;
        int newWidth = 0;
        for (BufferedImage image : images) {
            // 横向
            if (type == 1) {
                newHeight = Math.max(newHeight, image.getHeight());
                newWidth += image.getWidth();
            } else if (type == 2) {// 纵向
                newWidth = Math.max(newWidth, image.getWidth());
                newHeight += image.getHeight();
            }
        }
        if (type == 1 && newWidth < 1) {
            return null;
        }
        if (type == 2 && newHeight < 1) {
            return null;
        }
        // 生成新图片
        BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        int height_i = 0;
        int width_i = 0;
        for (int i = 0; i < images.length; i++) {
            if (type == 1) {
                ImageNew.setRGB(width_i, 0, images[i].getWidth(), newHeight, ImageArrays[i], 0,
                        images[i].getWidth());
                width_i += images[i].getWidth();
            } else if (type == 2) {
                ImageNew.setRGB(0, height_i, newWidth, images[i].getHeight(), ImageArrays[i], 0, newWidth);
                height_i += images[i].getHeight();
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(ImageNew, "png", os);
        return os;
    }
}
