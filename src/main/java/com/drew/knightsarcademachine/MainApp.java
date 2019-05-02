package com.drew.knightsarcademachine;

import static com.drew.knightsarcademachine.FXMLController.gameDescription_;
import static com.drew.knightsarcademachine.FXMLController.gameList_;
import static com.drew.knightsarcademachine.FXMLController.gamePic_;
import static com.drew.knightsarcademachine.FXMLController.pic_;
import static com.drew.knightsarcademachine.ProgressWindowController.progressBar_;
import static com.drew.knightsarcademachine.ProgressWindowController.progressLabel_;
import com.google.gson.Gson;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;


public class MainApp extends Application {

    private static S3Client s3;
    String gameFile;
    String justFile;
    boolean pressed = false;
    boolean pressed2 = false;
    boolean gameRunning = false;
    boolean ready = true;
    boolean closeOK = true;
    boolean native3Press = false;
    boolean not3Press = false;
    static Stage mainStage;
    Stage stage3;
    Timer timer;
    Timer timer2;

    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        
        stage.setTitle("Knights Arcade");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
        
        mainStage = stage;
        
        File file = new File("background.jpg");
        Image image = new Image(file.toURI().toString());
        pic_.setImage(image);
        
        stage.setOnCloseRequest(event -> {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException e1) {
                e1.printStackTrace();
            }
        });
        
        mainStage.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
          @Override
          public void changed(ObservableValue<? extends Boolean> ov, Boolean onHidden, Boolean onShown)
          {
            if(onShown) {
                System.out.println("main window gained focus and ready is: " + ready + " pressed is: " + pressed);
                
            }
          }
        });
        
        root.setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent e) {
                if (e.isControlDown() && e.getCode() == KeyCode.P) {
                    
                    Task<Parent> pullTask = new Task<Parent>() {
                        @Override
                        public Parent call() {
                            try {
                    
                                double workDone = 0.0;

                                updateMessage("Pulling all game data from knightsarcade.com");

                                //Sending request for game data
                                String url = "http://knightsarcade.com/api/v1/Public/rds/games/allapprovedgames";

                                URL obj = new URL(url);
                                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                                con.setRequestMethod("GET");

                                int responseCode = con.getResponseCode();
                                //System.out.println("\nSending 'GET' request to URL : " + url);
                                //System.out.println("Response Code : " + responseCode);

                                BufferedReader in = new BufferedReader(
                                    new InputStreamReader(con.getInputStream()));
                                String inputLine;
                                StringBuffer response = new StringBuffer();

                                while ((inputLine = in.readLine()) != null) {
                                        response.append(inputLine);
                                }
                                in.close();

                                //print result
                                //System.out.println(response.toString());

                                //Turning Json Objects into Java Objects
                                workDone += 100;
                                updateProgress(workDone, 1000);
                                updateMessage("Building game data objects");
                                Gson gson = new Gson();

                                Game[] gamesArray = gson.fromJson(response.toString(), Game[].class);

                                Region region = Region.US_EAST_2;
                                
                                AwsBasicCredentials awsCreds = AwsBasicCredentials.create("AKIAW6JAATVADFP3GP6U","d8lDC8/J5PZ/+k1HO0uq/Y+c7eyrM8IC5gRs+Zk3");
                                s3 = S3Client.builder().region(region).credentialsProvider(StaticCredentialsProvider.create(awsCreds)).build();
                                
                                //s3 = S3Client.builder().region(region).build();

                                workDone += 50;
                                updateProgress(workDone, 1000);

                                double indieGameProgress = (double)800/gamesArray.length;

                                for(int i = 0; i < gamesArray.length; i++) {
                                    //Creating Directory for each game
                                    updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Checking for existing data");
                                    File f = new File("./GameData/" + gamesArray[i].getGameName());
                                    if (!f.exists() && !f.isDirectory()) {
                                        double step = indieGameProgress / 5;

                                        updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Building directory");
                                        f.mkdirs();

                                        workDone += step;
                                        updateProgress(workDone, 1000);
                                        updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Downloading picture from storage");

                                        System.out.println(workDone);
                                        System.out.println(i);

                                        String bucket = "knightsarcades3";
                                        String key = "public/" + gamesArray[i].getGamePicPath();

                                        //Getting picture and game Amazon S3
                                        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),ResponseTransformer.toFile(Paths.get(f.getPath() + File.separator + "display.png")));

                                        workDone += step;
                                        updateProgress(workDone, 1000);
                                        updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Downloading game files from storage");

                                        s3.getObject(GetObjectRequest.builder().bucket(bucket).key("public/" + gamesArray[i].getGameFilePath()).build(),ResponseTransformer.toFile(Paths.get(f.getPath() + File.separator + "game.zip")));

                                        workDone += step;
                                        updateProgress(workDone, 1000);
                                        updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Writing game description to file");

                                        //Writing the game description to a .txt file
                                        PrintWriter writer = new PrintWriter(f.getPath() + File.separator + "desc.txt", "UTF-8");
                                        writer.print(gamesArray[i].getGameDescription());
                                        writer.close();

                                        workDone += step;
                                        updateProgress(workDone, 1000);
                                        updateMessage("Game " + (i + 1) + " of " + gamesArray.length + ": " + '"' + gamesArray[i].getGameName() + '"' + " - Unzipping game files");

                                        //Unzipping game
                                        byte[] byteBuffer = new byte[1024];

                                        try{
                                            ZipInputStream inZip = new ZipInputStream(new FileInputStream(f.getPath() + File.separator + "game.zip"));
                                            ZipEntry inZipEntry = inZip.getNextEntry();
                                            while(inZipEntry != null){
                                                String fileName = inZipEntry.getName();
                                                File unZippedFile = new File(f.getPath() + File.separator + fileName);
                                                System.out.println("Unzipping: " + unZippedFile.getAbsoluteFile());
                                                if (inZipEntry.isDirectory()){
                                                    unZippedFile.mkdirs();
                                                }else{
                                                    new File(unZippedFile.getParent()).mkdirs();
                                                    unZippedFile.createNewFile();
                                                    FileOutputStream unZippedFileOutputStream = new FileOutputStream(unZippedFile);
                                                    int length;
                                                    while((length = inZip.read(byteBuffer)) > 0){
                                                        unZippedFileOutputStream.write(byteBuffer,0,length);
                                                    }
                                                    unZippedFileOutputStream.close();                       
                                                }
                                                inZipEntry = inZip.getNextEntry(); 
                                            }
                                            //inZipEntry.close();
                                            inZip.close();
                                            System.out.println("Finished Unzipping");
                                        }catch(IOException e2){
                                            e2.printStackTrace();
                                        }

                                        workDone += step;
                                        updateProgress(workDone, 1000);

                                    }
                                    else {
                                        workDone += indieGameProgress;
                                        updateProgress(workDone, 1000);
                                    }
                                }

                                updateMessage("Cleaning up GameData directory and filling UI list");

                                System.out.println("done");

                                String dirPath = "./GameData";
                                File dir = new File(dirPath);
                                if(dir.exists()) {

                                    String[] files = dir.list();
                                    if (files.length == 0) {
                                        System.out.println("The directory is empty");
                                    } else {
                                        for (String aFile : files) {
                                            gameList_.getItems().add(aFile);
                                        }
                                    }

                                }

                                gameList_.getSelectionModel().select(0);

                                workDone += 50;
                                updateProgress(workDone, 1000);

                            } catch (MalformedURLException ex) {
                                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            return null;
                        }
                    };
                    
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ProgressWindow.fxml"));
                    Parent root1 = null;
                    try {
                        root1 = (Parent) fxmlLoader.load();
                    } catch (IOException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Stage stage2 = new Stage();
                    stage2.setScene(new Scene(root1));
                    stage2.setTitle("Pulling games from www.KnightsArcade.com");
                    stage2.setResizable(false);
                    
                    pullTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent event) {
                            stage2.close();
                        }
                    });
                    
                    progressBar_.progressProperty().bind(pullTask.progressProperty());
                    progressLabel_.textProperty().bind(pullTask.messageProperty());
                    
                    Thread gamePullThread = new Thread(pullTask);
                    gamePullThread.start();
                    
                    stage2.show();
                    
                    /*System.out.println("hi");
                    //new File("./path/directory").mkdirs();

                    try {
                        //Sending request for game data
                        String url = "http://knightsarcade.com/api/v1/Public/rds/games/allapprovedgames";

                        URL obj = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        con.setRequestMethod("GET");

                        int responseCode = con.getResponseCode();
                        //System.out.println("\nSending 'GET' request to URL : " + url);
                        //System.out.println("Response Code : " + responseCode);

                        BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                        }
                        in.close();

                        //print result
                        //System.out.println(response.toString());

                        //Turning Json Objects into Java Objects
                        Gson gson = new Gson();

                        Game[] newArray = gson.fromJson(response.toString(), Game[].class);
                        
                        Region region = Region.US_EAST_2;
                        s3 = S3Client.builder().region(region).build();
                        
                        for(int i = 0; i < newArray.length; i++) {
                            //Creating Directory for each game
                            File f = new File("./GameData/" + newArray[i].getGameName());
                            if (!f.exists() && !f.isDirectory()) {
                                f.mkdirs();
                                
                                System.out.println(i);
                                
                                String bucket = "knightsarcades3";
                                String key = "public/" + newArray[i].getGamePicPath();
                        
                                //Getting picture and game Amazon S3
                                s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),ResponseTransformer.toFile(Paths.get(f.getPath() + File.separator + "display.png")));
                                s3.getObject(GetObjectRequest.builder().bucket(bucket).key("public/" + newArray[i].getGameFilePath()).build(),ResponseTransformer.toFile(Paths.get(f.getPath() + File.separator + "game.zip")));
                                
                                //Writing the game description to a .txt file
                                PrintWriter writer = new PrintWriter(f.getPath() + File.separator + "desc.txt", "UTF-8");
                                writer.print(newArray[i].getGameDescription());
                                writer.close();
                                
                                //Unzipping game
                                byte[] byteBuffer = new byte[1024];

                                try{
                                    ZipInputStream inZip = new ZipInputStream(new FileInputStream(f.getPath() + File.separator + "game.zip"));
                                    ZipEntry inZipEntry = inZip.getNextEntry();
                                    while(inZipEntry != null){
                                        String fileName = inZipEntry.getName();
                                        File unZippedFile = new File(f.getPath() + File.separator + fileName);
                                        System.out.println("Unzipping: " + unZippedFile.getAbsoluteFile());
                                        if (inZipEntry.isDirectory()){
                                            unZippedFile.mkdirs();
                                        }else{
                                            new File(unZippedFile.getParent()).mkdirs();
                                            unZippedFile.createNewFile();
                                            FileOutputStream unZippedFileOutputStream = new FileOutputStream(unZippedFile);
                                            int length;
                                            while((length = inZip.read(byteBuffer)) > 0){
                                                unZippedFileOutputStream.write(byteBuffer,0,length);
                                            }
                                            unZippedFileOutputStream.close();                       
                                        }
                                        inZipEntry = inZip.getNextEntry(); 
                                    }
                                    //inZipEntry.close();
                                    inZip.close();
                                    System.out.println("Finished Unzipping");
                                }catch(IOException e2){
                                    e2.printStackTrace();
                                }
                                
                            }
                        }
                        
                        System.out.println("done");
                        
                        String dirPath = "./GameData";
                        File dir = new File(dirPath);
                        if(dir.exists()) {

                            String[] files = dir.list();
                            if (files.length == 0) {
                                System.out.println("The directory is empty");
                            } else {
                                for (String aFile : files) {
                                    gameList_.getItems().add(aFile);
                                }
                            }

                        }
                        
                        gameList_.getSelectionModel().select(0);

                    } catch (MalformedURLException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                }
            }
        });
        
        FXMLLoader fxmlLoader2 = new FXMLLoader(getClass().getResource("/fxml/LoadingWindow.fxml"));
        Parent root2 = null;
        try {
            root2 = (Parent) fxmlLoader2.load();
        } catch (IOException ex) {
            Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        stage3 = new Stage();
        stage3.setScene(new Scene(root2));
        stage3.setResizable(false);
        stage3.initStyle(StageStyle.UNDECORATED);
        
        stage3.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
          @Override
          public void changed(ObservableValue<? extends Boolean> ov, Boolean onHidden, Boolean onShown)
          {
            if(onHidden) {
                
                timer = new Timer();
                timer.schedule(new CloseTask(), 3000);
                timer2 = new Timer();
                timer2.schedule(new TimeoutTask(), 300000);
                stage3.close();
                mainStage.toBack();
                
                try {
                    GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException ex) {
                    System.err.println("There was a problem registering the native hook.");
                    System.err.println(ex.getMessage());

                    System.exit(1);
		}
            }
          }
        });
        
        gameList_.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent ke)
            {
                System.out.println(ke.getCharacter());
                if (ke.getCode().equals(KeyCode.DIGIT3) && !pressed && ready)
                {
                    System.out.println("hi");
                    pressed = true;
                    ready = false;
                    closeOK = false;
                    stage3.show();
                    Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
                    stage3.setX((primScreenBounds.getWidth() - stage3.getWidth()) / 2);
                    stage3.setY((primScreenBounds.getHeight() - stage3.getHeight()) / 2);
                    
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        runtime.exec(gameFile);
                    } catch (IOException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (ke.getCode().equals(KeyCode.DIGIT3)){
                    System.out.println("oops");
                    System.out.println("ready is " + ready);
                    System.out.println("pressed is " + pressed);
                }
            }
        });
        
        gameList_.setOnKeyReleased(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent ke)
            {
                System.out.println(ke.getCharacter());
                if (ke.getCode().equals(KeyCode.DIGIT3))
                {
                    pressed = false;
                }
            }
        });
        
        String dirPath = "./GameData";
        File dir = new File(dirPath);
        if(dir.exists()) {
            
            String[] files = dir.list();
            if (files.length == 0) {
                System.out.println("The directory is empty");
            } else {
                for (String aFile : files) {
                    gameList_.getItems().add(aFile);
                }
            }
        
        }
        
        //gameList_.setCellFactory(stringListView -> new CenteredListViewCell());
        
        gameList_.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                try {
                    // Your action here
                    gameDescription_.setText("");
                    File file = new File("./GameData/" + newValue + "/display.png");
                    Image image = new Image(file.toURI().toString());
                    gamePic_.setImage(image);
                    
                    Scanner s = new Scanner(new File("./GameData/" + newValue + "/desc.txt")).useDelimiter("\\s+");
                    while (s.hasNext()) {
                        if (s.hasNextInt()) { // check if next token is an int
                            gameDescription_.appendText(s.nextInt() + " "); // display the found integer
                        } else {
                            gameDescription_.appendText(s.next() + " "); // else read the next token
                        }
                    }
                    
                    File dir = new File("./GameData/" + newValue);
                    String[] extensions = new String[] {"exe"};
                    List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
                    File temp = files.get(0);
                    gameFile = temp.getPath();
                    //gameFile = files.get(0).getPath();
                    justFile = temp.getName();
                    
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }

        });
        
        gameList_.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                
                if(!newValue) {
                    gameList_.requestFocus();
                }
                
            }
            
        });
        
        gameList_.requestFocus();
        gameList_.getSelectionModel().select(0);

        // Clear previous logging configurations.
        LogManager.getLogManager().reset();

        // Get the logger for "org.jnativehook" and set the level to off.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        GlobalScreen.addNativeKeyListener(new GlobalKeyListener());
        
    }
    
    public class GlobalKeyListener implements NativeKeyListener {
	public void nativeKeyPressed(NativeKeyEvent e) {
		System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
                
                if (e.getKeyCode() != NativeKeyEvent.VC_3 && !not3Press) {
                    not3Press = true;
                    timer2.cancel();
                    timer2 = new Timer();
                    timer2.schedule(new TimeoutTask(), 300000);
                }

		if (e.getKeyCode() == NativeKeyEvent.VC_3 && !native3Press && closeOK) {
                    native3Press = true;
                    timer2.cancel();
                    try {
                        File application = new File(gameFile);
                        String fullApplicationName = application.getName();
                        String applicationName = FilenameUtils.removeExtension(fullApplicationName);
                        System.out.println(applicationName);
                        
                        if(isProcessRunning(applicationName))
                        {
                            System.out.println("process was running");
                            Runtime rt = Runtime.getRuntime();
                            rt.exec("taskkill /F /T /IM " + '"' + fullApplicationName + '"');
                            System.out.println(fullApplicationName);
                            
                            ready = true;
                            pressed = false;
                            System.out.println(ready);
                            Platform.runLater( () -> {
                                mainStage.setIconified(false);
                                mainStage.toFront();
                                mainStage.setAlwaysOnTop(true);
                                mainStage.setAlwaysOnTop(false);
                                mainStage.requestFocus();
                            });
                            try {
                                GlobalScreen.unregisterNativeHook();
                                System.out.println("unregistered");
                            } catch (NativeHookException e1) {
                                e1.printStackTrace();
                            }
                            
                        } else {
                            ready = true;
                            pressed = false;
                            Platform.runLater( () -> {
                                mainStage.setIconified(false);
                                mainStage.toFront();
                                mainStage.setAlwaysOnTop(true);
                                mainStage.setAlwaysOnTop(false);
                                mainStage.requestFocus();
                            });
                            try {
                                GlobalScreen.unregisterNativeHook();
                                System.out.println("unregistered");
                            } catch (NativeHookException e1) {
                                e1.printStackTrace();
                            }
                        }
                        
                    } catch (IOException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
		}
	}

	public void nativeKeyReleased(NativeKeyEvent e) {
            System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
            
            if (e.getKeyCode() != NativeKeyEvent.VC_3) {
                not3Press = false;
            }
            
            if (e.getKeyCode() == NativeKeyEvent.VC_3) {
                native3Press = false;
            }
	}

	public void nativeKeyTyped(NativeKeyEvent e) {
		System.out.println("Key Typed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}

    }
    
    public static void runApplication(String applicationFilePath) throws IOException, InterruptedException
    {
        File application = new File(applicationFilePath);
        String applicationName = application.getName();

        if (!isProcessRunning(applicationName))
        {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(applicationFilePath);
            } catch (IOException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // http://stackoverflow.com/a/19005828/3764804
    private static boolean isProcessRunning(String processName) throws IOException, InterruptedException
    {
        /*String line;
        String pidInfo ="";

        Process p =Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe");

        BufferedReader input =  new BufferedReader(new InputStreamReader(p.getInputStream()));

        while ((line = input.readLine()) != null) {
            pidInfo+=line; 
        }

        input.close();
        
        System.out.println(pidInfo);
        
        return pidInfo.contains(processName);*/
        
        ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
        Process process = processBuilder.start();
        String tasksList = toString(process.getInputStream());
        
        System.out.println(tasksList);

        return tasksList.contains(processName);
    }

    // http://stackoverflow.com/a/5445161/3764804
    private static String toString(InputStream inputStream)
    {
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        String string = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        return string;
    }
    
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        launch(args);
        
    }
    
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return destFile;
    }
    
    class CenteredListViewCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                // Create the HBox
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.CENTER);

                // Create centered Label
                Label label = new Label(item);
                label.setAlignment(Pos.CENTER);

                hBox.getChildren().add(label);
                setGraphic(hBox);
            }
        }
    }
    
    class CloseTask extends TimerTask {
        public void run() {
            System.out.println("Time's up!");
            closeOK = true;
            timer.cancel(); //Terminate the timer thread
        }
    }
    
    class TimeoutTask extends TimerTask {
        public void run() {
            System.out.println("closing since nobody is playing");
            
            try {
                File application = new File(gameFile);
                String fullApplicationName = application.getName();
                String applicationName = FilenameUtils.removeExtension(fullApplicationName);
                System.out.println(applicationName);

                if(isProcessRunning(applicationName))
                {
                    System.out.println("process was running");
                    Runtime rt = Runtime.getRuntime();
                    rt.exec("taskkill /F /T /IM " + '"' + fullApplicationName + '"');
                    System.out.println(fullApplicationName);

                    ready = true;
                    pressed = false;
                    System.out.println(ready);
                    Platform.runLater( () -> {
                        mainStage.requestFocus();
                        mainStage.toFront();
                        mainStage.setAlwaysOnTop(true);
                        mainStage.setAlwaysOnTop(false);
                        //mainStage.setIconified(true);
                        mainStage.setIconified(false);
                    });
                    try {
                        GlobalScreen.unregisterNativeHook();
                        System.out.println("unregistered");
                    } catch (NativeHookException e1) {
                        e1.printStackTrace();
                    }

                } else {
                    ready = true;
                    pressed = false;
                    Platform.runLater( () -> {
                        mainStage.requestFocus();
                        mainStage.toFront();
                        mainStage.setAlwaysOnTop(true);
                        mainStage.setAlwaysOnTop(false);
                        //mainStage.setIconified(true);
                        mainStage.setIconified(false);
                    });
                    try {
                        GlobalScreen.unregisterNativeHook();
                        System.out.println("unregistered");
                    } catch (NativeHookException e1) {
                        e1.printStackTrace();
                    }
                }

            } catch (IOException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            timer2.cancel(); //Terminate the timer thread
        }
    }
    
    class Game{
        private int gameId;
        private String gameName;
        private String gameCreatorId;
        private String gameCreatorName;
        private String gameDescription;
        private String gameControls;
        private String gameVideolink;
        private Boolean gameGenreSurvival;
        private Boolean gameGenreFighting;
        private Boolean gameGenrePuzzle;
        private Boolean gameGenrePlatformer;
        private Boolean gameGenreShooter;
        private Boolean gameGenreStrategy;
        private Boolean gameGenreSports;
        private Boolean gameGenreRpg;
        private Boolean gameGenreRacing;
        private Boolean gameGenreAdventure;
        private Boolean gameGenreAction;
        private Boolean gameGenreRhythm;
        private String gameStatus;
        private Boolean gameOnArcade;
        private String gamePath;
        private String[] gameImg;
        private String gameSubmissionDateUtc;
        private String gameReviewDateUtc;
        
        String getGameName() {
            return gameName;
        }
        
        String getGameDescription() {
            return gameDescription;
        }
        
        String getGamePicPath() {
            return gameImg[0];
        }
        
        String getGameFilePath() {
            return gamePath;
        }
        
    }

}