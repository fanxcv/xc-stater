package fun.fan.xc.starter.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import fun.fan.xc.starter.exception.XcToolsException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public class PdfTools {
    public static ByteArrayOutputStream addTextOrImage(InputStream inputPDFFile, AddElementInfo... elements)
            throws Exception {
        Assert.notNull(elements, "待添加元素不能为空");
        PdfReader reader = new PdfReader(inputPDFFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream);
        // int i = reader.getNumberOfPages() + 1;
        BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        // PdfGState gs = new PdfGState();
        // gs.setFillOpacity(0.2f);

        // content = stamper.getOverContent(i);// 在内容上方加水印

        Arrays.stream(elements).forEach(item -> {
            try {
                PdfContentByte content = stamper.getUnderContent(item.page);// 在内容下方加水印
                content.beginText();
                if (item.type == ElementType.TEXT) {
                    TextElement text = item.text;
                    // 字体大小
                    content.setFontAndSize(base, text.fontSize);
                    // content.setTextMatrix(70, 200);
                    // 内容居中，横纵坐标，偏移量
                    content.showTextAligned(text.align, text.text, item.x, item.y, 0);
                } else if (item.type == ElementType.IMAGE) {
                    ImageElement image = item.image;
                    // 将image对象添加到content中
                    Image img = Objects.nonNull(image.bytes) ? Image.getInstance(image.bytes) : Image.getInstance(image.path);
                    img.setAbsolutePosition(item.x, item.y);
                    img.scaleToFit(image.width, image.height);
                    content.addImage(img);
                }
                content.endText();
            } catch (IOException | DocumentException e) {
                throw new XcToolsException(e);
            }
        });

        // //添加图片
        // Image image = Image.getInstance("D:\\测试图片.jpg");
        //
        // /*
        //   img.setAlignment(Image.LEFT | Image.TEXTWRAP);
        //   img.setBorder(Image.BOX); img.setBorderWidth(10);
        //   img.setBorderColor(BaseColor.WHITE); img.scaleToFit(100072);//大小
        //   img.setRotationDegrees(-30);//旋转
        //  */
        // //图片的位置（坐标）
        // image.setAbsolutePosition(520, 786);
        // // image of the absolute
        // image.scaleToFit(200, 200);
        // image.scalePercent(15);//依照比例缩放
        // content.addImage(image);

        stamper.close();
        // 关闭打开的原来PDF文件，不执行reader.close()删除不了（必须先执行stamper.close()，否则会报错）
        reader.close();
        inputPDFFile.close();
        return byteArrayOutputStream;
    }

    // public static BufferedImage[] toImage(byte[] pdf, int dpi)
    //         throws Exception {
    //     if (Objects.isNull(pdf)) {
    //         return null;
    //     }
    //     PDDocument document = Loader.loadPDF(pdf);
    //     PDFRenderer reader = new PDFRenderer(document);
    //     int pages = document.getNumberOfPages();
    //     BufferedImage[] res = new BufferedImage[pages];
    //     for (int i = 0; i < pages; i++) {
    //         res[i] = reader.renderImageWithDPI(i, dpi);
    //     }
    //     return res;
    // }


    @SneakyThrows
    public static void main(String[] args) {
        FileInputStream stream = new FileInputStream("/Users/fan/Downloads/a.pdf");
        AddElementInfo[] elements = {
                // 协议编号
                new AddElementInfo(1, 387, 692).setText(new TextElement("杨凡")),
                // 年
                new AddElementInfo(1, 387, 662).setText(new TextElement("2023")),
                // 月
                new AddElementInfo(1, 430, 662).setText(new TextElement("11")),
                // 日
                new AddElementInfo(1, 458, 662).setText(new TextElement("11")),
                // 购买人
                new AddElementInfo(1, 425, 630).setText(new TextElement("杨凡")),
                // 身份证号
                new AddElementInfo(1, 370, 598).setText(new TextElement("511602199411114890")),
                // 联系方式
                new AddElementInfo(1, 360, 567).setText(new TextElement("13111221111")),
                // 微信号
                new AddElementInfo(1, 347, 536).setText(new TextElement("fanxcvm")),
                // 联系方式
                new AddElementInfo(1, 395, 505).setText(new TextElement("13111221111")),
                // 添加图片
                new AddElementInfo(1, 195, 505).setImage(new ImageElement("/Users/fan/Downloads/jb.png", 200, 300)),
        };
        ByteArrayOutputStream os = addTextOrImage(stream, elements);
        // FileUtil.writeBytes(os.toByteArray(), "/Users/fan/Downloads/b.pdf");
        // BufferedImage[] image = toImage(os.toByteArray(), 200);
        // os = ImageTools.mergeImage(image, 1);
        FileUtil.writeBytes(os.toByteArray(), "/Users/fan/Downloads/b.png");
    }

    public enum ElementType {
        TEXT, IMAGE
    }

    @Data
    @RequiredArgsConstructor
    @Accessors(chain = true)
    public static class AddElementInfo {
        private final int page;
        private final int x;
        private final int y;
        private ElementType type = ElementType.TEXT;
        private ImageElement image;
        private TextElement text;

        public AddElementInfo setImage(ImageElement image) {
            this.type = ElementType.IMAGE;
            this.image = image;
            return this;
        }

        public AddElementInfo setText(TextElement text) {
            this.type = ElementType.TEXT;
            this.text = text;
            return this;
        }
    }

    @Data
    @RequiredArgsConstructor
    @Accessors(chain = true)
    public static class TextElement {
        private final String text;
        private int align = Element.ALIGN_LEFT;
        private int fontSize = 12;
    }

    @Data
    @Accessors(chain = true)
    public static class ImageElement {
        private final byte[] bytes;
        private final String path;
        private final int height;
        private final int width;

        public ImageElement(String path, int width, int height) {
            this.bytes = null;
            this.path = path;
            this.width = width;
            this.height = height;
        }

        public ImageElement(byte[] bytes, int width, int height) {
            this.bytes = bytes;
            this.path = null;
            this.width = width;
            this.height = height;
        }
    }
}
