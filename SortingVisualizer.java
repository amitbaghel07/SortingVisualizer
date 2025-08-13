import java.awt.*;
import java.util.Random;
import javax.swing.*;

/**
 * SortingVisualizer.java
 *
 * Single-file Swing application that visualizes multiple sorting algorithms.
 * Compile: javac SortingVisualizer.java
 * Run:     java SortingVisualizer
 *
 * Works with Java 8+.
 */
public class SortingVisualizer extends JFrame {
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 600;

    private final VisualPanel visualPanel;
    private JComboBox<String> algoBox;
    private JButton startButton;
    private JButton shuffleButton;
    private JSlider speedSlider;
    private JSlider sizeSlider;
    private volatile boolean running = false;
    private int[] array;

    public SortingVisualizer() {
        super("Sorting Visualizer - Amit baghel");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);

        array = generateArray(80);

        visualPanel = new VisualPanel();
        visualPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT - 120));
        add(visualPanel, BorderLayout.CENTER);

        add(createControls(), BorderLayout.SOUTH);

        pack();
        setVisible(true);
    }

    private JPanel createControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        algoBox = new JComboBox<>(new String[] {
                "Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Quick Sort"
        });
        panel.add(new JLabel("Algorithm:"));
        panel.add(algoBox);

        startButton = new JButton("Start");
        shuffleButton = new JButton("Shuffle");
        panel.add(startButton);
        panel.add(shuffleButton);

        panel.add(new JLabel("Speed:"));
        speedSlider = new JSlider(0, 200, 40); // lower -> faster (we will invert)
        speedSlider.setPreferredSize(new Dimension(140, 20));
        panel.add(speedSlider);

        panel.add(new JLabel("Size:"));
        sizeSlider = new JSlider(10, 300, array.length);
        sizeSlider.setPreferredSize(new Dimension(140, 20));
        panel.add(sizeSlider);

        // Actions
        startButton.addActionListener(e -> {
            if (!running) {
                running = true;
                startButton.setText("Stop");
                disableControlsWhileRunning(true);
                new Thread(this::runSelectedSort).start();
            } else {
                // Stop requested
                running = false;
                startButton.setText("Start");
                disableControlsWhileRunning(false);
            }
        });

        shuffleButton.addActionListener(e -> {
            if (running) return;
            array = generateArray(sizeSlider.getValue());
            visualPanel.resetState();
            visualPanel.repaint();
        });

        sizeSlider.addChangeListener(e -> {
            if (running) return;
            array = generateArray(sizeSlider.getValue());
            visualPanel.resetState();
            visualPanel.repaint();
        });

        return panel;
    }

    private void disableControlsWhileRunning(boolean disable) {
        algoBox.setEnabled(!disable);
        shuffleButton.setEnabled(!disable);
        sizeSlider.setEnabled(!disable);
    }

    private int[] generateArray(int n) {
        Random r = new Random();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = 10 + r.nextInt(480); // heights between 10..490
        return a;
    }

    private int getDelayFromSlider() {
        // speedSlider 0..200: map to delay ms, smaller slider = faster. Avoid 0 ms entirely.
        int val = speedSlider.getValue();
        int delay = Math.max(1, 201 - val); // 1..201
        return delay;
    }

    private void runSelectedSort() {
        String algo = (String) algoBox.getSelectedItem();
        visualPanel.setArray(array);
        try {
            switch (algo) {
                case "Bubble Sort" -> bubbleSort();
                case "Selection Sort" -> selectionSort();
                case "Insertion Sort" -> insertionSort();
                case "Merge Sort" -> mergeSort(0, array.length - 1);
                case "Quick Sort" -> quickSort(0, array.length - 1);
            }
            // final state: mark sorted
            visualPanel.markSorted();
            visualPanel.repaint();
        } catch (InterruptedException ex) {
            // Thread interrupted -> stop
        } finally {
            running = false;
            SwingUtilities.invokeLater(() -> {
                startButton.setText("Start");
                disableControlsWhileRunning(false);
            });
        }
    }

    // Utility to pause & repaint; throws InterruptedException to respect stop
    private void pauseAndRepaint(int i, int j) throws InterruptedException {
        visualPanel.highlight(i, j);
        visualPanel.repaint();
        Thread.sleep(getDelayFromSlider());
        if (!running) throw new InterruptedException();
    }

    // Sorting algorithms with visual hooks
    private void bubbleSort() throws InterruptedException {
        int n = array.length;
        for (int i = 0; i < n - 1 && running; i++) {
            for (int j = 0; j < n - 1 - i && running; j++) {
                pauseAndRepaint(j, j + 1);
                if (array[j] > array[j + 1]) {
                    swap(j, j + 1);
                    pauseAndRepaint(j, j + 1);
                }
            }
        }
    }

    private void selectionSort() throws InterruptedException {
        int n = array.length;
        for (int i = 0; i < n - 1 && running; i++) {
            int min = i;
            for (int j = i + 1; j < n && running; j++) {
                pauseAndRepaint(min, j);
                if (array[j] < array[min]) min = j;
            }
            if (min != i) {
                swap(min, i);
                pauseAndRepaint(min, i);
            }
        }
    }

    private void insertionSort() throws InterruptedException {
        int n = array.length;
        for (int i = 1; i < n && running; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j] > key && running) {
                pauseAndRepaint(j, j + 1);
                array[j + 1] = array[j];
                j--;
                pauseAndRepaint(j + 1, j + 2);
            }
            array[j + 1] = key;
            pauseAndRepaint(j + 1, i);
        }
    }

    private void mergeSort(int l, int r) throws InterruptedException {
        if (!running) return;
        if (l < r) {
            int m = l + (r - l) / 2;
            mergeSort(l, m);
            mergeSort(m + 1, r);
            merge(l, m, r);
        }
    }

    private void merge(int l, int m, int r) throws InterruptedException {
        int n1 = m - l + 1;
        int n2 = r - m;
        int[] L = new int[n1];
        int[] R = new int[n2];
        System.arraycopy(array, l, L, 0, n1);
        System.arraycopy(array, m + 1, R, 0, n2);
        int i = 0, j = 0, k = l;
        while (i < n1 && j < n2 && running) {
            pauseAndRepaint(k, (i + l));
            if (L[i] <= R[j]) {
                array[k++] = L[i++];
            } else {
                array[k++] = R[j++];
            }
        }
        while (i < n1 && running) {
            array[k++] = L[i++];
            pauseAndRepaint(k - 1, l);
        }
        while (j < n2 && running) {
            array[k++] = R[j++];
            pauseAndRepaint(k - 1, l);
        }
    }

    private void quickSort(int low, int high) throws InterruptedException {
        if (!running) return;
        if (low < high) {
            int pi = partition(low, high);
            quickSort(low, pi - 1);
            quickSort(pi + 1, high);
        }
    }

    private int partition(int low, int high) throws InterruptedException {
        int pivot = array[high];
        int i = (low - 1);
        for (int j = low; j <= high - 1 && running; j++) {
            pauseAndRepaint(j, high);
            if (array[j] <= pivot) {
                i++;
                swap(i, j);
                pauseAndRepaint(i, j);
            }
        }
        swap(i + 1, high);
        pauseAndRepaint(i + 1, high);
        return i + 1;
    }

    private void swap(int i, int j) {
        int t = array[i];
        array[i] = array[j];
        array[j] = t;
        visualPanel.setArray(array);
    }

    // Visual Panel inner class
    class VisualPanel extends JPanel {
        private int[] localArray;
        private int highlightA = -1, highlightB = -1;
        private boolean sorted = false;

        VisualPanel() {
            setBackground(Color.BLACK);
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        }

        void setArray(int[] arr) {
            this.localArray = arr;
            sorted = false;
        }

        void highlight(int a, int b) {
            highlightA = a;
            highlightB = b;
        }

        void resetState() {
            highlightA = highlightB = -1;
            sorted = false;
        }

        void markSorted() {
            sorted = true;
            highlightA = highlightB = -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (localArray == null || localArray.length == 0) return;
            int w = getWidth();
            int h = getHeight();
            int n = localArray.length;
            int barWidth = Math.max(1, w / n);

            // draw bars
            for (int i = 0; i < n; i++) {
                int val = localArray[i];
                int barHeight = (int) ((val / 500.0) * h); // our values are up to ~490
                int x = i * barWidth;
                int y = h - barHeight;

                if (sorted) {
                    g.setColor(new Color(50, 205, 50)); // lime-ish for sorted
                } else if (i == highlightA || i == highlightB) {
                    g.setColor(new Color(255, 69, 0)); // orange-red for active
                } else {
                    // gradient based on height
                    float hue = 0.6f * (float) val / 500f; // blue -> cyan-ish
                    g.setColor(Color.getHSBColor(hue, 0.9f, 0.9f));
                }

                g.fillRect(x, y, barWidth - 1, barHeight);
            }

            // overlay text
            g.setColor(Color.WHITE);
            g.drawString("Items: " + n + "    Speed(ms): " + getDelayFromSlider()
                    + "    Algorithm: " + algoBox.getSelectedItem()
                    + (running ? "    (running)" : ""), 8, 16);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SortingVisualizer());
    }
}
