// This class rapresents the section of the
// webcam described by the rectangle

public class Section {
  private int x1,x2,y1,y2,area;
  private boolean motion;
  public int pixelsChanged,threeStepVar,movements;
  
  public Section(int x1,int x2,int y1,int y2) {
    if(x1<x2) {
      this.x1=x1;
      this.x2=x2;
    } else {
      this.x1=x2;
      this.x2=x1;
    }
    if(y1<y2) {
      this.y1=y1;
      this.y2=y2;
    } else {
      this.y1=y2;
      this.y2=y1;
    }
    this.area = (this.x2-this.x1) * (this.y2-this.y1);
  }
  
  public int x1() {return x1;}
  public int x2() {return x2;}
  public int y1() {return y1;}
  public int y2() {return y2;}
  
  public boolean isMotion() {return motion;}
  public void setMotion(boolean b) {motion=b;}
  
  public int area() {return area;}
  
  public void onFirstMove(int i) {
    String pr = "/usr/bin/zenity --info --text=\""+i+"\"";
    try {
      Runtime.getRuntime().exec(pr);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
