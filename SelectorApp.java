package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.security.Key;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import selector.SelectionModel.SelectionState;
import scissors.ScissorsSelectionModel;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

        // Add status bar
        statusLabel = new JLabel();
        Container cont = frame.getContentPane();
        statusLabel.setFont(new Font("Arcade", Font.ITALIC, 16));
        cont.add(statusLabel, BorderLayout.SOUTH);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imgPanel);//Wrapping the image in the scroll pane
        cont.add(scrollPane);//Must add the scroll pane that contains the image panel

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        JPanel jPanel = makeControlPanel();
        cont.add(jPanel, BorderLayout.EAST);

        // Controller: Set initial selection tool and update components to reflect its state
        frame.pack();//Must remember to pack at the end
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setMnemonic(KeyEvent.VK_ENTER);//Alt + Enter to open
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);

        saveItem = new JMenuItem("Save...");
        saveItem.setMnemonic(KeyEvent.VK_S);//Alt + s to save
        saveItem.addActionListener(e -> saveSelection());
        fileMenu.add(saveItem);

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setMnemonic(KeyEvent.VK_C);//Alt + c to cancel
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        fileMenu.add(closeItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);
        exitItem.addActionListener(e -> frame.dispose());
        exitItem.setMnemonic(KeyEvent.VK_E);//Alt + E to exit

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        undoItem.setMnemonic(KeyEvent.VK_U);//Alt + U to undo
        undoItem.addActionListener(e -> model.undo());
        editMenu.add(undoItem);

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel jpanel = new JPanel();
        jpanel.setLayout(new BoxLayout(jpanel, BoxLayout.Y_AXIS));
        jpanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] models = new String[]{"Point-to-Point", "Intelligent Scissors",
                "SimpleColorWeight"};
        JComboBox<String> jComboBox = new JComboBox<>(models);
        jComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension uniformSize = new Dimension(200, 30);// Uniform size for all components
        jComboBox.setMaximumSize(uniformSize);
        jComboBox.setPreferredSize(uniformSize); // Set the preferred size for JComboBox
        jComboBox.addActionListener(e -> {
            String selectedItem = (String) jComboBox.getSelectedItem();
            switch (selectedItem) {
                case "Point-to-Point":
                    setSelectionModel(new PointToPointSelectionModel(model));
                    break;
                case "Intelligent Scissors":
                    setSelectionModel(new ScissorsSelectionModel("CrossGradMono", model));
                    break;
                case "SimpleColorWeight":
                    setSelectionModel(new ScissorsSelectionModel("SimpleColorWeight", model));
                    break;
            }
        });
        jpanel.add(Box.createRigidArea(new Dimension(0, 10)));
        jpanel.add(jComboBox);

        //Modified the shape and the sizes of the buttons
        cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(uniformSize);
        cancelButton.setMaximumSize(uniformSize);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> model.cancelProcessing());
        jpanel.add(Box.createRigidArea(new Dimension(0, 5)));
        jpanel.add(cancelButton);

        undoButton = new JButton("Undo");
        undoButton.setPreferredSize(uniformSize);
        undoButton.setMaximumSize(uniformSize);
        undoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        undoButton.addActionListener(e -> model.undo());
        jpanel.add(Box.createRigidArea(new Dimension(0, 5)));
        jpanel.add(undoButton);

        resetButton = new JButton("Reset");
        resetButton.setPreferredSize(uniformSize);
        resetButton.setMaximumSize(uniformSize);
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.addActionListener(e -> model.reset());
        jpanel.add(Box.createRigidArea(new Dimension(0, 5)));
        jpanel.add(resetButton);

        finishButton = new JButton("Finish");
        finishButton.setPreferredSize(uniformSize);
        finishButton.setMaximumSize(uniformSize);
        finishButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        finishButton.addActionListener(e -> model.finishSelection());
        jpanel.add(Box.createRigidArea(new Dimension(0, 5)));
        jpanel.add(finishButton);

        return jpanel;
    }


    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include: * "state":
     * Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            reflectSelectionState(model.state());
            if (model.state() == PROCESSING) {
                processingProgress.setIndeterminate(true);
            } else {
                processingProgress.setValue(0);
                processingProgress.setIndeterminate(false);
            }
        }
        if (evt.getPropertyName().equals("progress")) {
            processingProgress.setIndeterminate(false);
            processingProgress.setValue((Integer) evt.getNewValue());
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        if (state == NO_SELECTION) {
            cancelButton.setEnabled(false);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
        }
        if (state == PROCESSING) {
            cancelButton.setEnabled(true);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
        }
        if (state == SELECTING) {
            finishButton.setEnabled(true);
            saveItem.setEnabled(false);
            cancelButton.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
        }
        if (state == SELECTED) {
            saveItem.setEnabled(true);
            undoButton.setEnabled(true);
            resetButton.setEnabled(true);
        }
        //Must be rigorously tested
        statusLabel.setText(state.toString());
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);
        model.addPropertyChangeListener("progress", this);
        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));
        chooser.setDialogTitle("Open File");
        while (true) {
            int val = chooser.showOpenDialog(frame);
            if (val != JFileChooser.APPROVE_OPTION) {
                return;
            }
            BufferedImage img;
            try {
                img = ImageIO.read(chooser.getSelectedFile());
                if (img != null) {
                    this.setImage(img);
                    break;
                } else {
                    JOptionPane.showMessageDialog(frame,
                            "No image has been passed in the file" +
                                    chooser.getSelectedFile().getPath(), "Invalid Image",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.frame, "Could not read the image at "
                                + chooser.getSelectedFile().getPath(), "Unsupported Image Format",
                        JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog. Show
     * an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
        chooser.setDialogTitle("save file");
        while (true) {
            //There is an issue with the cancel button. I don't know where the error is occurring.
            int val = chooser.showSaveDialog(frame);
            if (val == JFileChooser.APPROVE_OPTION) {
                //Setting the file path to .png
                File file = chooser.getSelectedFile();
                String filePath = file.getAbsolutePath();
                if (!filePath.endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                if (file.exists()) {
                    int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to "
                            + "continue?", "Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        try {

                            BufferedImage originalImg = imgPanel.image();
                            Polygon clips = PolyLine.makePolygon(
                                    this.getSelectionModel().selection());
                            Rectangle bounds = clips.getBounds();
                            clips.translate(-bounds.x, -bounds.y);
                            BufferedImage img = new BufferedImage(bounds.width, bounds.height,
                                    BufferedImage.TYPE_INT_ARGB);
                            var g = img.createGraphics();
                            //Setting the clips
                            g.setClip(clips);
                            //By drawing the image on top of the clips, we essentially get the clipped portion
                            g.drawImage(originalImg, 0, 0, null);
                            //We then write the clipped image on to the selected file
                            ImageIO.write(img, "png", file);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(frame, e.getMessage(),
                                    e.getClass().toString(),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } else if (response == JOptionPane.NO_OPTION) {
                        continue;
                    } else if (response == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                } else {
                    try {
                        BufferedImage originalImg = imgPanel.image();
                        Polygon clips = PolyLine.makePolygon(this.getSelectionModel().selection());
                        Rectangle bounds = clips.getBounds();
                        clips.translate(-bounds.x, -bounds.y);
                        BufferedImage img = new BufferedImage(bounds.width, bounds.height,
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics g = img.createGraphics();
                        g.setClip(clips);
                        g.drawImage(originalImg, 0, 0, null);
                        g.dispose();
                        ImageIO.write(img, "png", file);
                        break; // Exit loop after successful save
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(frame, e.getMessage(),
                                e.getClass().toString(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (val == JFileChooser.CANCEL_OPTION) {
                return; // Exit the method if the cancel button is pressed
            }
        }

    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}
