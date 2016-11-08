package ciugen.ui;

//import com.dyson.chart.data.xyz.DoubleXYZDataSeries;
//import com.dyson.chart.plot.Axis;
//import com.dyson.chart.plot.heatmap.HeatMap;
import java.awt.BorderLayout;
import javax.swing.JPanel;

public class ChartPanel
  extends JPanel
{
  //private HeatMap heatmap;
  
  public ChartPanel()
  {
    initComponents();
  }
  
  private void initComponents()
  {
    setLayout(new BorderLayout());
  }
  /*
  public void setData(DoubleXYZDataSeries data)
  {
    if (getComponentCount() > 0) {
      remove(0);
    }
    this.heatmap = new HeatMap("Heat Map", data, "Trap Collision Energy(V)", "Drift Time (Bins)", "Values", true, true);
    add(this.heatmap);
    this.heatmap.getXAxis().setShowMajorTicks(false);
  }
  
  public void setCEStep(double step)
  {
    this.heatmap.setxStepSize((int)step);
    this.heatmap.resetZoom();
  }
  */
}
