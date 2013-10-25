import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import codeanticode.gsvideo.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MotionDetection extends PApplet {



GSCapture cam;
int numPixels;
int[] backgroundPixels;
int detected = 0;
ArrayList<Section> toSee;
int sumDiff;

int w = 640;
int h = 480;
// 0.5% = 5/1000
float threshold = 0.5f;

// FLAGS

/* 0 = no save
 * 1 = save 1/1 fps
 * 2 = save 1/2 fps 
 * 3 = save 1/3 fps
 * ... 
 */
int fSAVE = 0;

/* 0 = false
 * 1 = true
 */
int fDEBUG = 0;

/* 0 = false
 * 1 = true
 */
int fCONTINUOUS = 1;

/* 0 = false
 * 1 = true
 */
int fCOLOR = 1;

boolean fCALIBRATING = false;
// ----

public int area(int[] a) {
  return abs(a[0]-a[2])*abs(a[1]-a[3]);
}

public boolean detectMovement(int rx) {
  Section s = toSee.get(rx);
  if(s.pixelsChanged <= s.area()/100.0f*threshold) {
    s.threeStepVar=0;
    s.setMotion(false);
    return false;
  }
  else {
    if (s.threeStepVar >= 3) {
      s.setMotion(true);
      return true;
    } else {
      s.threeStepVar++;
      s.setMotion(false);
      return false;
    }
  }
}

public void setup() {
  size(w, h);
  cam = new GSCapture(this, w, h, "/dev/video0");
  cam.start();

  numPixels = cam.width * cam.height;
  backgroundPixels = new int[numPixels];
  loadPixels();
  cam.loadPixels();
  arraycopy(cam.pixels, backgroundPixels);
  
  textFont(createFont("Arial",12,true));
  
  toSee = new ArrayList<Section>();
}

public void drawRectProp(int rx) {
  if(rx>=toSee.size()) {return;}
  noStroke();
  rectMode(CORNERS);

  Section s = toSee.get(rx);
  String str = "Area: " + s.area()
  +"\nMotion: " + (s.isMotion() ? "yes" : "no")
  +"\n" + s.pixelsChanged + "p "
  +"\n" + s.pixelsChanged*100.0f/s.area() + "%"
  ;  
  fill(0, 150);
  rect(100*rx+0, 0, 100*rx+100, 100);
  fill(255);
  text(str, 100*rx+5, 5, 100*rx+100, 100);
  
}

public void drawFlags() {
  noStroke();
  rectMode(CORNERS);

  String str = "s) Saving rate: " + (fSAVE!=0 ? 1.0f/fSAVE : 0)
  + "\nd) Deubg: " + (fDEBUG==1 ? "yes" : "no")
  + "\nc) Color: " + (fCOLOR==1 ? "yes" : "no")
  + "\nk) Continuous: " + (fCONTINUOUS==1 ? "yes" : "no")
  ;
  fill(0, 150);
  rect(0, height-(20*str.split("\n").length)-5, 110, height);
  fill(255);
  text(str, 5, height-(20*str.split("\n").length), 110, height);
  
  for(int i=0; i<toSee.size(); i++) {
    drawRectProp(i);
  }
}

public void draw() {
  if (cam.available()) {    
    cam.read();
    cam.loadPixels();
    loadPixels();

    if(fCALIBRATING) {
      background(cam);
      updatePixels();
    }
    
    int currentRect;
    int presenceSum = 0;
    for(currentRect=0; currentRect<toSee.size(); currentRect++) {
      Section s = toSee.get(currentRect);
      s.pixelsChanged = 0;
      sumDiff = 0;
      for(int tempy=s.y1(); tempy<s.y2(); tempy++) {
        for(int tempx=s.x1(); tempx<s.x2(); tempx++) {
          int i = tempx+tempy*width;
          if(i<0 || i>=cam.pixels.length) {continue;}
          
          int currColor = cam.pixels[i];
          int bkgdColor = backgroundPixels[i];
        
          int currR = (currColor >> 16) & 0xFF;
          int currG = (currColor >> 8) & 0xFF;
          int currB = currColor & 0xFF;
          
          int bkgdR = (bkgdColor >> 16) & 0xFF;
          int bkgdG = (bkgdColor >> 8) & 0xFF;
          int bkgdB = bkgdColor & 0xFF;
          
          int diffR = abs(currR - bkgdR);
          int diffG = abs(currG - bkgdG);
          int diffB = abs(currB - bkgdB);
          
          int diff = diffR + diffG + diffB;
          //sumDiff += diff;
          if (diff>50) {
            pixels[i] = color(255,255,255);
            s.pixelsChanged++;
          }
          else {
            if(fCOLOR == 0) {
              pixels[i]=color(0,0,0);
            } else {
              pixels[i]=backgroundPixels[i];
            }
          }
        }
      }
      if(detectMovement(currentRect)) {
        if(fSAVE!=0 && detected%fSAVE==0) {
          cam.save((detected/fSAVE)+".png");
          if(fDEBUG==1) {println("Saved in "+(detected/fSAVE)+".png");}
        }
        detected++;
      }
      updatePixels();
    }

    if(fCONTINUOUS==1) {
      arraycopy(cam.pixels, backgroundPixels);
    }
    drawFlags();
  }
}

public void keyPressed() {
  switch(key) {
  case 's':
    fSAVE = fSAVE==10 ? 0 : 10;
    break;
  case 'd':
    fDEBUG = fDEBUG==1 ? 0 : 1;
    break;
  case 'k':
    fCONTINUOUS = fCONTINUOUS==1 ? 0 : 1;
    break;
  case 'K':
    arraycopy(cam.pixels, backgroundPixels);
    break;
  case 'c':
    fCOLOR = fCOLOR==1 ? 0 : 1;
    break;
  case 'w':
    fCALIBRATING = !fCALIBRATING;
    break;
  case 'l':
    toSee = new ArrayList<Section>();
    background(150);
    draw();
  }
}

int x, y;
public void mousePressed() {
  if(!fCALIBRATING) {return;}
  x = mouseX;
  y = mouseY;
}

public void mouseDragged() {
  if(!fCALIBRATING) {return;}
  updatePixels();
  background(150);
  fill(255,0,0,150);
  stroke(255,0,0);
  rectMode(CORNERS);
  rect(x,y,mouseX,mouseY);
}

public void mouseReleased() {
  if(!fCALIBRATING) {return;}
  toSee.add(new Section(x,mouseX,y,mouseY));
  if(fDEBUG==1) {println("Saved rect "+x+";"+y+" -> "+mouseX+";"+mouseY);}
}
// This class rapresents the section of the
// webcam described by the rectangle

public class Section {
  private int x1,x2,y1,y2,area;
  private boolean motion;
  public int pixelsChanged,threeStepVar;
  
  public Section(int x1,int x2,int y1,int y2) {
    if(x1<x2) {
      this.x1=x1;
      this.x2=x2;
    } else {
      this.x2=x1;
      this.x1=x2;
    }
    if(y1<y2) {
      this.y1=y1;
      this.y2=y2;
    } else {
      this.y2=y1;
      this.y1=y2;
    }
   this.area = (x2-x1) * (y2-y1);
  }
  
  public int x1() {return x1;}
  public int x2() {return x2;}
  public int y1() {return y1;}
  public int y2() {return y2;}
  
  public boolean isMotion() {return motion;}
  public void setMotion(boolean b) {motion=b;}
  
  public int area() {return area;}
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MotionDetection" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
