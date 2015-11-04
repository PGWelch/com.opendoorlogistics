package com.opendoorlogistics.components.geocode.postcodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;

@XmlRootElement
public class PCImporterConfig extends PCDatabaseSelectionConfig {
	/**
	 * 
	 */
	private static final long serialVersionUID = -285475347888278427L;
	private int level;

	public int getLevel() {
		return level;
	}

	@XmlAttribute
	public void setLevel(int level) {
		this.level = level;
	}

	static JPanel createConfigEditorPanel(final ComponentConfigurationEditorAPI factory,PCImporterConfig config, String operationName) {

		class MyPanel extends PCGeocoderDatabaseSelectionPanel {

			MyPanel(final PCImporterConfig pcConfig) {
				super(factory.getApi(),pcConfig);
				addWhitespace();
				add(new JLabel(operationName + " postcodes from level:"));

				final JFormattedTextField level = new JFormattedTextField();
				level.setValue(new Integer(pcConfig.getLevel()));
				level.setColumns(10);
				level.addPropertyChangeListener("value", new PropertyChangeListener() {

					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						int ilevel = ((Number) level.getValue()).intValue();
						pcConfig.setLevel(ilevel);
					}
				});
				add(level);
				addWhitespace();

			}

		}

		return new MyPanel((PCImporterConfig) config);
	}
}