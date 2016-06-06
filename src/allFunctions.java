import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by error404 Team on 5/27/16.
 */
public abstract class allFunctions {
    private static String configEmail, configEmailPassword, configEmailHost, configEmailPort;
    private static int patientID;
    private static String patientFirstName;
    private static String patientLastName;
    private static String fileName;
    private static String dbHost;
    private static String dbUser;
    private static String dbPassword;
    private static int dbDriver;
    private static String dbTableName;
    private static String timeStampString;
    private enum COLUMNS {id, fname, lname, ss}

    /**
     * Setter method for fileName
     * @param fileName String input parameter that will set the file name
     */
    public static void setFileName(String fileName) {
        allFunctions.fileName = fileName;
    }

    /**
     * Getter method for fileName
     * @return String; the file name
     */
    public static String getFileName() {
        return fileName;
    }


    public static void setTimeStampString(String timeStampString) {
        allFunctions.timeStampString = timeStampString;
    }

    /**
     * Default email smtp server configuration
     */

    public static void defaultConfiguration(){
        configEmail = "openemr.healthrecord@gmail.com";
        configEmailPassword = "Cook68^put^^";
        configEmailHost = "smtp.gmail.com";
        configEmailPort = "587";
    }

    /**
     * Method for loading data from DB file. Used for testing purposes
     * @param id int; patient's id
     * @param firstName String; patient's first name
     * @param lastName String; patient's last name
     * @param subdirectory String; the folder name withing the project where the db file is
     * @param fullFileName String; the fileName within the subDirectory; If different directory, state the full name
     * @return true if generated successfully, otherwise false
     * @throws Exception if error occured wile generating the CCD file
     */
    public static boolean generateFromDBFile(int id, String firstName, String lastName, String subdirectory, String fullFileName) throws Exception {
        Scanner fileScan = loadCSV(subdirectory,fullFileName);
        String tempFName, tempLName, tempSSN, tempDOB;
        int tempID;
        fileScan.useDelimiter(","); // setting the delimiter
        fileScan.nextLine();// skipping the first line since it is the header
        while(fileScan.hasNextLine()){
            tempID = Integer.parseInt(fileScan.next());
            tempFName = fileScan.next();
            tempLName = fileScan.next();
            tempDOB = fileScan.next();
            tempSSN = fileScan.next();
            if(tempID==id && tempFName.equals(firstName) && tempLName.equals(lastName)){
                CCDGenerator ccdGenerator = new CCDGenerator(tempID+"",tempFName,tempLName,tempDOB,fileScan.next(),fileScan.next(),fileScan.next(),fileScan.next());
                tempSSN=tempSSN.substring(7,11);
                String timeStamp = ccdGenerator.getTimeStampString();
                fileName = ccdGenerator.getFileName();
                CryptoUtils.encrypt(tempSSN+timeStamp, new File(fileName),
                        new File(fileName));
                System.out.println("Record found!");
                fileScan.close();
                return true;
            }
            fileScan.nextLine();
        }
        System.err.println("Record not found!");
        fileScan.close();
        return false;
    }

    /**
     * Load the CSV testing file which is located in testData folder within the project directory
     * @param subDirectory String; the folder name
     * @param fileName String; the file name
     * @return
     */
    private static Scanner loadCSV(String subDirectory, String fileName) {
        String path = System.getProperty("user.dir") + File.separator + subDirectory + File.separator;
        Scanner input = null;
        try {
            input = new Scanner(new File(path + fileName));
        } catch (FileNotFoundException e) {
            System.err.println("The file is not found!");
        }
        return input;
    }

    /**
     * Method for generating CCD file from the database
     * @param id queried patient id
     * @param firstName queried patient first name
     * @param lastName queried patient last name
     * @return true if extraction is successful, otherwise fals
     * @throws Exception if there was an error in extracting the CCD file
     */
    public static boolean generateRecord(int id, String firstName, String lastName) throws Exception{
        patientID=id;
        patientFirstName =firstName;
        patientLastName=lastName;
        ResultSet resultSet = ConnectNQuery.connectNQuery(dbHost,dbUser,dbPassword,dbDriver,
                "select * from "+dbTableName+" where "+ COLUMNS.id+"="+patientID+" && "+ COLUMNS.fname+"='"+ patientFirstName +"' && " +
                        COLUMNS.lname+"='"+patientLastName+"'");
        if(resultSet.next()){
            new CCDGenerator(resultSet);
            String ssn = resultSet.getString(COLUMNS.ss.toString());
            CryptoUtils.encrypt(ssn.substring(ssn.length()-4,ssn.length())+ timeStampString, new File(fileName),new File(fileName));
            return true;
        }
        return false;
    }

    /**
     * Method for setting up the database configuration
     * @param host the database location
     * @param user administrative/root user
     * @param password administrative/root password
     * @param driver 1- Oracle db driver 2-MySQL db driver
     * @param tableName the table name that information will be extracted (patient_data in this case)
     */
    public static void loadDBConfig(String host, String user, String password, int driver, String tableName){
        dbHost=host;
        dbUser=user;
        dbPassword=password;
        dbDriver=driver;
        dbTableName=tableName;
    }

    /**
     * Getter method for patient's first name
     * @return String
     */
    public static String getPatientFirstName() {
        return patientFirstName;
    }

    /**
     * Getter method for patient's last name
     * @return String
     */
    public static String getPatientLastName() {
        return patientLastName;
    }

    /**
     * Getter method for patient's ID
     * @return int
     */
    public static int getPatientID() {
        return patientID;
    }

    /**
     * Setter method for the patient's last name
     * @param patientLastName String
     */
    public static void setPatientLastName(String patientLastName) {
        allFunctions.patientLastName = patientLastName;
    }

    /**
     * Setter method for patient's first name
     * @param patientFirstName String
     */
    public static void setPatientFirstName(String patientFirstName) {
        allFunctions.patientFirstName = patientFirstName;
    }

    /**
     * Setter method for the patient's ID
     * @param patientID int
     */
    public static void setPatientID(int patientID) {
        allFunctions.patientID = patientID;
    }

    /**
     * Method for checking if a string is an integer
     * @param string String that has been checked
     * @return true if the string is integer, otherwise false
     */
    public static boolean isInteger(String string){
        try {
            Integer.parseInt(string);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * Method for sending the already extracted and encrypted patient record
     * @param to the destination email
     * @param subject the email subject
     * @param tempRecord the patient's record
     */
    public static void sendEMail(String to, String subject, String tempRecord){

        //Mail properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", configEmailHost);
        props.put("mail.smtp.port", configEmailPort);

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator(){
                    protected PasswordAuthentication getPasswordAuthentication(){
                        return new PasswordAuthentication(configEmail, configEmailPassword);
                    }
                });

        try {
            // Default MimeMessage object.
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(configEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            //MimeBodyPart
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("This email contains the health record of the patient "+ patientFirstName +" "+patientLastName+".");

            //MimeMultipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            messageBodyPart = new MimeBodyPart();

            //Attachment
            File record = new File(tempRecord);
            DataSource source = new FileDataSource(record);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(record.getName());
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            Transport.send(message);
            Files.delete(record.toPath());
            System.out.println("Email sent!");

        } catch (MessagingException e) {
            System.err.println(e.getCause());
        } catch (IOException e) {
            System.err.println(e.getCause());
        }
    }
}
