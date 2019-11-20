import static com.itextpdf.text.pdf.BaseFont.EMBEDDED;
import static com.itextpdf.text.pdf.BaseFont.IDENTITY_H;
import static org.thymeleaf.templatemode.TemplateMode.HTML;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.airtel.prod.engg.email.AttachmentInfo;
import com.airtel.prod.engg.email.EmailMessage;
import com.airtel.prod.engg.email.EmailUtils;

/**
 * This is a JUnit test which will generate a PDF using Flying Saucer
 * and Thymeleaf templates. The PDF will display a letter styled with
 * CSS. The letter has two pages and will contain text and images.
 * <p>
 * Simply run this test to generate the PDF. The file is called:
 * <p>
 * /test.pdf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:META-INF/email-test-context.xml"})
public class FlyingSaucerTest {

    private static final String OUTPUT_FILE = "test.csv";
    private static final String UTF_8 = "UTF-8";
    
    @Autowired
	private EmailUtils utils;
	
	@Value("${mail.sender}")
	private String sender;
	
	@Value("#{'${mail.recepients}'.split(',')}") 
	private List<String> toList;
	
	@Value("#{'${mail.cc.recipient}'.split(',')}") 
	private List<String> ccList;
	
	@Value("#{'${mail.bcc.recipient}'.split(',')}") 
	private List<String> bccList;
	
	@Value("${mail.title}")
	private String mailTitle;
	
	private static String delim=",";

    @Test
    public void generatePdf() throws Exception {

        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
        // are HTML-based files located under "src/test/resources". Beside
        // of the main HTML file, we also have partials like a footer or
        // a header. We can re-use those partials in different documents.
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        // The data in our Thymeleaf templates is not hard-coded. Instead,
        // we use placeholders in our templates. We fill these placeholders
        // with actual data by passing in an object. In this example, we will
        // write a letter to "John Doe".
        //
        // Note that we could also read this data from a JSON file, a database
        // a web service or whatever.
        Data data = exampleDataForJohnDoe();
        
        List<Product> products = products();
        

        Context context = new Context();
        context.setVariable("data", data);
        context.setVariable("prods", products);

        // Flying Saucer needs XHTML - not just normal HTML. To make our life
        // easy, we use JTidy to convert the rendered Thymeleaf template to
        // XHTML. Note that this might not work for very complicated HTML. But
        // it's good enough for a simple letter.
        String renderedHtmlContent = templateEngine.process("template", context);
        String xHtml = convertToXhtml(renderedHtmlContent);
        
        String subject = "Mike testing 123...";
//		String body = "Hi building the email utility please help in testing and use this utility with attachment";
//		String body = "<!DOCTYPE html>\n" + 
//				"<html>\n" + 
//				"<body>\n" + 
//				"\n" + 
//				"<h1>My First Heading</h1>\n" + 
//				"\n" + 
//				"<p>My first paragraph.</p>\n" + 
//				"\n" + 
//				"</body>\n" + 
//				"</html>";
		
		List<AttachmentInfo> attachments = new ArrayList<AttachmentInfo>();
		AttachmentInfo attachment1 = new AttachmentInfo();
		StringBuffer buff = new StringBuffer();
		buff.append("Region"+delim+"Circle"+delim+"Area"+"\n");
		buff.append("ABC"+delim+"BCD"+delim+"DEF"+"\n");
		buff.append("GHI"+delim+"HIJ"+delim+"IJK"+"\n");
		String csv = buff.toString();
		attachment1.setAttachmentFileContent(csv.getBytes());
		attachment1.setAttachmentMimeType("text/csv");
		attachment1.setFileNameForAttachment("testDoc.csv");
		
		AttachmentInfo attachment2 = new AttachmentInfo();
		attachment2.setFilePath("/Users/b0096703/Desktop/permission_req.txt");
		attachment2.setFileNameForAttachment("testDoc.txt");
		
		attachments.add(attachment1);
		attachments.add(attachment2);
		
//		EmailMessage message = new EmailMessage(sender, toList, ccList, bccList, subject, body, "text/html",filePath,fileNameForAttachment, mailTitle);
		
		String fileNameForAttachment = "testDoc.csv";
		
		EmailMessage message = new EmailMessage(sender, 
				toList,
				ccList, 
				bccList, 
				subject, 
				xHtml, 
				"text/html", 
				attachments, mailTitle);
		boolean emailSentStatus = utils.sendEmail(message);
		Assert.assertEquals(emailSentStatus, true);
		System.out.println("ending the process...");

        ITextRenderer renderer = new ITextRenderer();
        renderer.getFontResolver().addFont("Code39.ttf", IDENTITY_H, EMBEDDED);

        // FlyingSaucer has a working directory. If you run this test, the working directory
        // will be the root folder of your project. However, all files (HTML, CSS, etc.) are
        // located under "/src/test/resources". So we want to use this folder as the working
        // directory.
        String baseUrl = FileSystems
                                .getDefault()
                                .getPath("src", "test", "resources")
                                .toUri()
                                .toURL()
                                .toString();
        renderer.setDocumentFromString(xHtml, baseUrl);
        renderer.layout();

        // And finally, we create the PDF:
        OutputStream outputStream = new FileOutputStream(OUTPUT_FILE);
        renderer.createPDF(outputStream);
        outputStream.close();
    }

    private Data exampleDataForJohnDoe() {
        Data data = new Data();
        data.setFirstname("John");
        data.setLastname("Doe");
        data.setStreet("Example Street 1");
        data.setZipCode("12345");
        data.setCity("Example City");
        return data;
    }
    
    private List<Product> products(){
    	List<Product> products = new ArrayList<FlyingSaucerTest.Product>();
    	Product p1 = new Product();
    	p1.setName("p1");
    	p1.setPrice("20");
    	p1.setInStock(true);
    	
    	Product p2 = new Product();
    	p2.setName("p2");
    	p2.setPrice("50");
    	p2.setInStock(false);
    	
    	products.add(p1);
    	products.add(p2);
    	return products;
    }

    static class Data {
        private String firstname;
        private String lastname;
        private String street;
        private String zipCode;
        private String city;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }
    
    static class Product{
    	private String name;
    	
    	private String price;
    	
    	private boolean inStock;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPrice() {
			return price;
		}

		public void setPrice(String price) {
			this.price = price;
		}

		public boolean isInStock() {
			return inStock;
		}

		public void setInStock(boolean inStock) {
			this.inStock = inStock;
		}
    	
    }

    private String convertToXhtml(String html) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tidy.parseDOM(inputStream, outputStream);
        return outputStream.toString(UTF_8);
    }

}
