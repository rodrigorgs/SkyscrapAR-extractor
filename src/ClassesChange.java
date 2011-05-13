package prefuse.demos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.GroupAction;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.DataShapeAction;
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.controls.ToolTipControl;
import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.io.DelimitedTextTableReader;
import prefuse.data.query.ListQueryBinding;
import prefuse.data.query.RangeQueryBinding;
import prefuse.data.query.SearchQueryBinding;
//import prefuse.demos.FilesChange.Counter;
import prefuse.render.AxisRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.render.AbstractShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.UpdateListener;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JRangeSlider;
import prefuse.util.ui.JSearchPanel;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.expression.VisiblePredicate;
import prefuse.visual.sort.ItemSorter;

public class ClassesChange extends JPanel {

    public static void main(String[] args) {
        UILib.setPlatformLookAndFeel();
        
        JFrame f = demo();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
    
    public static JFrame demo() {
        // load the data
        Table t = null;
        try {
            t = new DelimitedTextTableReader().readTable("/FilesChange2.txt");
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit(1);
        }
        
        JFrame frame = new JFrame("Change History");
        frame.setContentPane(new ClassesChange(t));
        frame.pack();
        return frame;
    }	
    
    // ------------------------------------------------------------------------
    
    private static final String TOTAL_CHANGES = "NumberOfChanges";
    private static final String CHANGES = "NumberOfChanges";
    
    private String m_title = "Change History";
    private String m_totalStr;
    private double m_totalChange = 10000;
    private int m_totalFiles = 100000;
    private JFastLabel m_total = new JFastLabel(m_totalFiles+" Files: "+m_totalChange);
    private JFastLabel m_details;
    
    private Visualization m_vis;
    private Display m_display;
    private Rectangle2D m_dataB = new Rectangle2D.Double();
    private Rectangle2D m_xlabB = new Rectangle2D.Double();
    private Rectangle2D m_ylabB = new Rectangle2D.Double();    
	
    public ClassesChange(Table t) {
        super(new BorderLayout());
        
        // --------------------------------------------------------------------
        // STEP 1: setup the visualized data
        
        final Visualization vis = new Visualization();
        m_vis = vis;
        
        final String group = "by_package";

        // filter to show only candidates receiving more than $100,000
        Predicate p = (Predicate)
            ExpressionParser.parse("["+TOTAL_CHANGES+"] >= 0"); 
        VisualTable vt = vis.addTable(group, t, p);
        
        // add a new column containing a label string showing
        // candidate name, party, state, year, and total receipts
        vt.addColumn("label", "CONCAT(FileName, ' (', "
                + "Package, "
                + "') ', FileType, ':', NumberOfChanges)");

        // add calculation for senators
        //vt.addColumn("Senate", "District <= 0");       
                
        vis.setRendererFactory(new RendererFactory() {
            AbstractShapeRenderer sr = new ShapeRenderer();
            Renderer arY = new AxisRenderer(Constants.RIGHT, Constants.TOP);
            Renderer arX = new AxisRenderer(Constants.CENTER, Constants.FAR_BOTTOM);
            
            public Renderer getRenderer(VisualItem item) {
                return item.isInGroup("ylab") ? arY :
                       item.isInGroup("xlab") ? arX : sr;
            }
        });
        
        // --------------------------------------------------------------------
        // STEP 2: create actions to process the visual data

        // set up dynamic queries, search set
        RangeQueryBinding  receiptsQ = new RangeQueryBinding(vt, CHANGES);
        ListQueryBinding   filesTypeQ    = new ListQueryBinding(vt, "FileType");
        SearchQueryBinding searchQ   = new SearchQueryBinding(vt, "FileName");
        
        // construct the filtering predicate
        AndPredicate filter = new AndPredicate(searchQ.getPredicate());
        filter.add(filesTypeQ.getPredicate());
        filter.add(receiptsQ.getPredicate());
        
        // set up the actions
        AxisLayout xaxis = new AxisLayout(group, "Package",
                Constants.X_AXIS, VisiblePredicate.TRUE); 
        AxisLayout yaxis = new AxisLayout(group, CHANGES,
                Constants.Y_AXIS, VisiblePredicate.TRUE);
        //yaxis.setScale(Constants.LOG_SCALE);
        yaxis.setRangeModel(receiptsQ.getModel());
        receiptsQ.getNumberModel().setValueRange(0,400,0,400);
        
        xaxis.setLayoutBounds(m_dataB);
        yaxis.setLayoutBounds(m_dataB);
        
        AxisLabelLayout ylabels = new AxisLabelLayout("ylab", yaxis, m_ylabB);
        //NumberFormat nf = NumberFormat.getCurrencyInstance();
        //nf.setMaximumFractionDigits(0);
        //ylabels.setNumberFormat(nf);
        
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        ylabels.setNumberFormat(nf);
        
        AxisLabelLayout xlabels = new AxisLabelLayout("xlab", xaxis, m_xlabB, 15);
        vis.putAction("xlabels", xlabels);
        
        // concrete = blue, abstract = green, other = gray, interface = red
        int[] palette = new int[] {
            ColorLib.rgb(20,255,20), ColorLib.rgb(100,100,255),
            ColorLib.rgb(255,100,100),/*ColorLib.rgb(180,180,180)*/ };

        DataColorAction color = new DataColorAction(group, "FileType",
                Constants.NOMINAL, VisualItem.STROKECOLOR, palette);
        
        //Tem que ver o que colocar para ser representado pelos shapes. Por enquanto coloquei
        //os tipos dos arquivos (classes).
        int[] shapes = new int[]
            { Constants.SHAPE_RECTANGLE, Constants.SHAPE_DIAMOND, Constants.SHAPE_CROSS,
        		Constants.SHAPE_STAR};
        DataShapeAction shape = new DataShapeAction(group, "FileType", shapes);
        
        Counter cntr = new Counter(group);
        
        ActionList draw = new ActionList();
        draw.add(cntr);
        draw.add(color);
        draw.add(shape);
        draw.add(xaxis);
        draw.add(yaxis);
        draw.add(ylabels);
        draw.add(new ColorAction(group, VisualItem.FILLCOLOR, 0));
        draw.add(new RepaintAction());
        vis.putAction("draw", draw);

        ActionList update = new ActionList();
        update.add(new VisibilityFilter(group, filter));
        update.add(cntr);
        update.add(xaxis);
        update.add(yaxis);
        update.add(ylabels);
        update.add(new RepaintAction());
        vis.putAction("update", update);
        
        UpdateListener lstnr = new UpdateListener() {
            public void update(Object src) {
                vis.run("update");
            }
        };
        filter.addExpressionListener(lstnr);
        
        // --------------------------------------------------------------------
        // STEP 4: set up a display and ui components to show the visualization

        m_display = new Display(vis);
        m_display.setItemSorter(new ItemSorter() {
            public int score(VisualItem item) {
                int score = super.score(item);
                if ( item.isInGroup(group) )
                    score += item.getInt(TOTAL_CHANGES);
                return score;
            }
        });
        m_display.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        m_display.setSize(700,450);
        m_display.setHighQuality(true);
        m_display.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                displayLayout();
            }
        });
        displayLayout();
        
        m_details = new JFastLabel(m_title);
        m_details.setPreferredSize(new Dimension(75,20));
        m_details.setVerticalAlignment(SwingConstants.BOTTOM);
        
        m_total.setPreferredSize(new Dimension(500,20));
        m_total.setHorizontalAlignment(SwingConstants.RIGHT);
        m_total.setVerticalAlignment(SwingConstants.BOTTOM);
        
        ToolTipControl ttc = new ToolTipControl("label");
        Control hoverc = new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent evt) {
                if ( item.isInGroup(group) ) {
                  m_total.setText(item.getString("label"));
                  item.setFillColor(item.getStrokeColor());
                  item.setStrokeColor(ColorLib.rgb(0,0,0));
                  item.getVisualization().repaint();
                }
            }
            public void itemExited(VisualItem item, MouseEvent evt) {
                if ( item.isInGroup(group) ) {
                  m_total.setText(m_totalStr);
                  item.setFillColor(item.getEndFillColor());
                  item.setStrokeColor(item.getEndStrokeColor());
                  item.getVisualization().repaint();
                }
            }
        };
        m_display.addControlListener(ttc);
        m_display.addControlListener(hoverc);
        
        
        // --------------------------------------------------------------------        
        // STEP 5: launching the visualization
        
        this.addComponentListener(lstnr);
        
        // details
        Box infoBox = new Box(BoxLayout.X_AXIS);
        infoBox.add(Box.createHorizontalStrut(5));
        infoBox.add(m_details);
        infoBox.add(Box.createHorizontalGlue());
        infoBox.add(Box.createHorizontalStrut(5));
        infoBox.add(m_total);
        infoBox.add(Box.createHorizontalStrut(5));
        
        // set up search box
        JSearchPanel searcher = searchQ.createSearchPanel();
        searcher.setLabelText("File: ");
        searcher.setBorder(BorderFactory.createEmptyBorder(5,5,5,0));
        
        // create dynamic queries
        Box radioBox = new Box(BoxLayout.X_AXIS);
        radioBox.add(Box.createHorizontalStrut(5));
        radioBox.add(searcher);
        radioBox.add(Box.createHorizontalGlue());
        radioBox.add(Box.createHorizontalStrut(5));
        radioBox.add(filesTypeQ.createRadioGroup());
        radioBox.add(Box.createHorizontalStrut(16));
        
        JRangeSlider slider = receiptsQ.createVerticalRangeSlider();
        slider.setThumbColor(null);
        slider.setMinExtent(150000);
        slider.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                m_display.setHighQuality(false);
            }
            public void mouseReleased(MouseEvent e) {
                m_display.setHighQuality(true);
                m_display.repaint();
            }
        });
        
        vis.run("draw");
        vis.run("xlabels");
        
        add(infoBox, BorderLayout.NORTH);
        add(m_display, BorderLayout.CENTER);
        add(slider, BorderLayout.EAST);
        add(radioBox, BorderLayout.SOUTH);
        UILib.setColor(this, ColorLib.getColor(255,255,255), Color.GRAY);
        slider.setForeground(Color.LIGHT_GRAY);
        UILib.setFont(radioBox, FontLib.getFont("Tahoma", 15));
        m_details.setFont(FontLib.getFont("Tahoma", 18));
        m_total.setFont(FontLib.getFont("Tahoma", 16));
    }
    
    public void displayLayout() {
        Insets i = m_display.getInsets();
        int w = m_display.getWidth();
        int h = m_display.getHeight();
        int iw = i.left+i.right;
        int ih = i.top+i.bottom;
        int aw = 85;
        int ah = 15;
        
        m_dataB.setRect(i.left, i.top, w-iw-aw, h-ih-ah);
        m_xlabB.setRect(i.left, h-ah-i.bottom, w-iw-aw, ah-10);
        m_ylabB.setRect(i.left, i.top, w-iw, h-ih-ah);
        
        m_vis.run("update");
        m_vis.run("xlabels");
    }
    
    private class Counter extends GroupAction {
        public Counter(String group) {
            super(group);
        }
        public void run(double frac) {
            double total = 0;
            int count = 0;
            VisualItem item = null;
            Iterator items = m_vis.visibleItems(m_group);
            while ( items.hasNext() ) {
                item = (VisualItem)items.next();
                total += item.getDouble("NumberOfChanges");
                ++count;
            }
            m_totalChange = total;
            m_totalFiles = count;
            
            if ( count == 1 ) {
                m_totalStr = item.getString("label");
            } else {
                m_totalStr = count + " Files changed " ;
            }
            m_total.setText(m_totalStr);
        }
    }
    
    
}
