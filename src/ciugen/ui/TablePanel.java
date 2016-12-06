package ciugen.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class TablePanel
extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3353912159273287385L;
	private JScrollPane jScrollPane1;
	private JTable jTable1;

	public TablePanel()
	{
		initComponents();
	}

	private void initComponents()
	{
		this.jScrollPane1 = new JScrollPane();
		this.jTable1 = new JTable();

		setLayout(new BorderLayout());

		this.jTable1.setModel(new DefaultTableModel(new Object[0][], new String[0]));

		this.jScrollPane1.setViewportView(this.jTable1);

		add(this.jScrollPane1, "Center");
	}

	public void setTableModel(DefaultTableModel model)
	{
		this.jTable1.setModel(model);
		revalidate();
		repaint();
	}
}
