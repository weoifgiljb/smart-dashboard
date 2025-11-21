package com.selfdiscipline.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.selfdiscipline.model.Diary;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class DiaryExportService {

    private String encodeUrl(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.URI uri = new java.net.URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri.toASCIIString();
        } catch (Exception e) {
            return urlStr;
        }
    }

    public void exportToPdf(List<Diary> diaries, OutputStream out) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Use Chinese font
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font font = new Font(bfChinese, 12, Font.NORMAL);
            
            for (Diary diary : diaries) {
                document.add(new Paragraph("Date: " + diary.getDiaryDate(), font));
                document.add(new Paragraph("Mood: " + diary.getMood(), font));
                document.add(new Paragraph("Content: " + diary.getContent(), font));
                
                if (diary.getImageUrl() != null && !diary.getImageUrl().isEmpty()) {
                    try {
                        String encodedUrl = encodeUrl(diary.getImageUrl());
                        com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(encodedUrl);
                        // Scale to fit page width
                        float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                        if (img.getScaledWidth() > maxWidth) {
                            img.scaleToFit(maxWidth, img.getScaledHeight() * (maxWidth / img.getScaledWidth()));
                        }
                        document.add(img);
                    } catch (Exception e) {
                        System.err.println("Failed to load image for PDF: " + e.getMessage());
                    }
                }

                document.add(new Paragraph("--------------------------------------------------"));
            }
        } catch (Exception e) {
            throw new IOException("Error generating PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    public void exportToWord(List<Diary> diaries, OutputStream out) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            for (Diary diary : diaries) {
                XWPFParagraph title = document.createParagraph();
                XWPFRun titleRun = title.createRun();
                titleRun.setBold(true);
                titleRun.setText("Date: " + diary.getDiaryDate() + " | Mood: " + diary.getMood());

                XWPFParagraph content = document.createParagraph();
                XWPFRun contentRun = content.createRun();
                contentRun.setText(diary.getContent());
                
                if (diary.getImageUrl() != null && !diary.getImageUrl().isEmpty()) {
                    try {
                        String encodedUrl = encodeUrl(diary.getImageUrl());
                        java.net.URL url = new java.net.URL(encodedUrl);
                        try (java.io.InputStream is = url.openStream()) {
                            byte[] bytes = is.readAllBytes();
                            
                            int format = XWPFDocument.PICTURE_TYPE_JPEG;
                            if (diary.getImageUrl().toLowerCase().endsWith(".png")) format = XWPFDocument.PICTURE_TYPE_PNG;
                            else if (diary.getImageUrl().toLowerCase().endsWith(".gif")) format = XWPFDocument.PICTURE_TYPE_GIF;
                            
                            XWPFParagraph imagePara = document.createParagraph();
                            XWPFRun imageRun = imagePara.createRun();
                            
                            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes)) {
                                java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(bais);
                                if (bi != null) {
                                    int width = bi.getWidth();
                                    int height = bi.getHeight();
                                    
                                    // Scale if too big (e.g. > 400px)
                                    int maxWidth = 400;
                                    if (width > maxWidth) {
                                        double ratio = (double) maxWidth / width;
                                        width = maxWidth;
                                        height = (int) (height * ratio);
                                    }
                                    
                                    imageRun.addPicture(new java.io.ByteArrayInputStream(bytes), format, "image", org.apache.poi.util.Units.toEMU(width), org.apache.poi.util.Units.toEMU(height));
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load image for Word: " + e.getMessage());
                    }
                }
                
                XWPFParagraph separator = document.createParagraph();
                separator.createRun().setText("--------------------------------------------------");
            }
            document.write(out);
        }
    }
}
