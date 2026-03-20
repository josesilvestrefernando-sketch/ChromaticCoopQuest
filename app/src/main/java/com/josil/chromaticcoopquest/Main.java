package com.josil.chromaticcoopquest;


import chickenfarm.Gridmap;
import processing.core.PApplet;

public class Main extends PApplet {


    int CELL_SIZE, totalwidth, tolaheight, GRID_SIZE_W, GRID_SIZE_H;

    Gridmap gridmap;
    public void settings() {

        fullScreen();
    }


    public void setup() {
        CELL_SIZE =40;
        totalwidth = width / CELL_SIZE;
        tolaheight = height / CELL_SIZE;
        GRID_SIZE_W = totalwidth;
        GRID_SIZE_H = (tolaheight);
        gridmap=new Gridmap(this,GRID_SIZE_W,GRID_SIZE_H,CELL_SIZE);
      
    }





    public void draw() {
       gridmap.draw();
    }

    public void mouseDragged(){
       gridmap.mousePressed();
    }
    public void mousePressed() {
        gridmap.mousePressed();
    }

}

