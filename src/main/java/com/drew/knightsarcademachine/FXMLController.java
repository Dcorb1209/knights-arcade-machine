package com.drew.knightsarcademachine;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXMLController implements Initializable {
    
    @FXML
    ListView gameList;
    
    @FXML
    ImageView gamePic;
    
    @FXML
    TextArea gameDescription;
    
    @FXML
    ImageView pic;
    
    
    static ListView gameList_;
    static ImageView gamePic_;
    static TextArea gameDescription_;
    static ImageView pic_;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameList_ = gameList;
        gamePic_ = gamePic;
        gameDescription_ = gameDescription;
        pic_ = pic;
        
        
    }    
}
