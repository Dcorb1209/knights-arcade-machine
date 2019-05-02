/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.drew.knightsarcademachine;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * FXML Controller class
 *
 * @author Drew
 */
public class ProgressWindowController implements Initializable {

    /**
     * Initializes the controller class.
     */
    
    @FXML
    Label progressLabel;
    
    @FXML
    ProgressBar progressBar;
    
    static Label progressLabel_;
    static ProgressBar progressBar_;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        progressLabel_ = progressLabel;
        progressBar_ = progressBar;
    }    
    
}