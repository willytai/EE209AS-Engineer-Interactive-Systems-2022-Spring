import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import processing.core.PApplet;
import processing.sound.AudioIn;
import processing.sound.FFT;
import processing.sound.Sound;
import processing.sound.Waveform;

/* A class with the main function and Processing visualizations to run the demo */

public class ClassifyVibration extends PApplet {
    // Set consts
    static int DEVICE_ID = 11;
    static int DISPLAY_LENGTH = 50;
    static int WINDOW_SIZE = 7;
    public String setTrainingDataFilename() { // Uses tempTrainingData if not defined
        return "./trainingData/ClassifyVibration_2022_04_24_11_35_47.trainingdata";
//        return "./trainingData/ClassifyVibration_2022_04_23_16_33_24.trainingdata";
    }

    public String[] setClassNames() {
        return new String[]{"quiet", "rock", "scissors"};
    }

    String tempTrainingData;

    String[] classNames;
    String trainingDataFilename;

    FFT fft;
    AudioIn in;
    Waveform waveform;
    int bands = 512;
    int nsamples = 1024 * 30;
    float[] spectrum = new float[bands];
    float[] fftFeatures = new float[bands];
    int classIndex = 0;
    int dataCount = 0;


    String prevOutput = "";
    int currCountDisplay = 0;
    List<String> windowList = new ArrayList<>();
    Map<String, Integer> windowMap = new HashMap<>();


    MLClassifier classifier;

    Map<String, List<DataInstance>> trainingData = new HashMap<>();


    DataInstance captureInstance(String label) {
        DataInstance res = new DataInstance();
        res.label = label;
        res.measurements = fftFeatures.clone();
        return res;
    }

    public static void main(String[] args) {
        PApplet.main("ClassifyVibration");
    }

    public void settings() {
        size(512, 400);
    }

    public void setup() {
        classNames = setClassNames();
        trainingDataFilename = setTrainingDataFilename();

        loadClasses(classNames);
        /* list all audio devices */
        Sound.list();
        Sound s = new Sound(this);

        /* select microphone device */
        println("Using device ID: " + DEVICE_ID);
        s.inputDevice(DEVICE_ID);

        /* create an Input stream which is routed into the FFT analyzer */
        fft = new FFT(this, bands);
        in = new AudioIn(this, 0);
        waveform = new Waveform(this, nsamples);
        waveform.input(in);

        /* start the Audio Input */
        in.start();

        /* patch the AudioIn */
        fft.input(in);
    }

    public void draw() {
        background(0);
        fill(0);
        stroke(255);

        waveform.analyze();

        beginShape();

        for (int i = 0; i < nsamples; i++) {
            vertex(
                    map(i, 0, nsamples, 0, width),
                    map(waveform.data[i], -1, 1, 0, height)
            );
        }

        endShape();

        fft.analyze(spectrum);

        for (int i = 0; i < bands; i++) {

            /* the result of the FFT is normalized */
            /* draw the line for frequency band i scaling it up by 40 to get more amplitude */
            line(i, height, i, height - spectrum[i] * height * 40);
            fftFeatures[i] = spectrum[i];
        }

        fill(255);
        textSize(30);

        if (classifier != null) {
            if (!prevOutput.equals("") && currCountDisplay < DISPLAY_LENGTH) {
                // If non-quiet output and still in display mode
                currCountDisplay += 1;
                text("classified as: " + prevOutput, 20, 30);
            } else if (currCountDisplay >= DISPLAY_LENGTH) {
                // Exceed display mode duration, reset stored window data
                currCountDisplay = 0;
                windowList.clear();
                for (String value : classNames) {
                    windowMap.put(value, 0);
                }
                prevOutput = "";
            } else {
                // Predict, only display output if window length is full
                String output = "";
                String guessedLabel = classifier.classify(captureInstance(null));
                windowList.add(guessedLabel);
                windowMap.put(guessedLabel, windowMap.get(guessedLabel) + 1);

                if (windowList.size() >= WINDOW_SIZE) {
                    output = getMaxKey(windowMap);
                    String oldest = windowList.get(0);
                    windowMap.put(oldest, windowMap.get(oldest) - 1);
                    windowList = windowList.subList(1, windowList.size());

                    // persist non-quiet output for DISPLAY_LENGTH
                    if (!output.equals("quiet")) {
                        prevOutput = output;
                    }
                }

                text("classified as: " + output, 20, 30);
            }
        } else {
            text(classNames[classIndex], 20, 30);
            dataCount = trainingData.get(classNames[classIndex]).size();
            text("Data collected: " + dataCount, 20, 60);
        }
    }

    public static String getMaxKey(Map<String, Integer> windowMap) {
        Map.Entry<String, Integer> maxEntry = null;
        for (Map.Entry<String, Integer> entry : windowMap.entrySet()) {
            if (maxEntry == null || entry.getValue()
                    .compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }

        assert maxEntry != null;
        return maxEntry.getKey();
    }

    public void keyPressed() {
        if (key == '.') {
            classIndex = (classIndex + 1) % classNames.length;
        } else if (key == 't') {
            if (classifier == null) {
                println("Start training ...");
                classifier = new MLClassifier();
                classifier.train(trainingData);
            } else {
                classifier = null;
            }
        } else if (key == 's') {
            // Yang: add code to save your trained model for later use
            println("Saving model");
            try {
                println("s key pressed");
                saveModel(trainingData);
            } catch (Exception e) {
                println(e);
            }
        } else if (key == 'l') {
            // Yang: add code to load your previously trained model
            try {
                println("l key pressed");
                trainingData = loadModel(trainingDataFilename);
                classifier = new MLClassifier();
                classifier.train(trainingData);
                println("Model loaded from " + trainingDataFilename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            trainingData.get(classNames[classIndex]).add(captureInstance(classNames[classIndex]));
        }
    }

    public void saveModel(Map<String, List<DataInstance>> trainingData) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("_yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();

        ObjectOutputStream oos = null;
        String filename = "./trainingData/" + this.getClass().getSimpleName() + dtf.format(now) + ".trainingdata";
        try {
            oos = new ObjectOutputStream(new FileOutputStream(filename));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        assert oos != null;
        oos.writeObject(trainingData);
        oos.flush();
        oos.close();
        println("Saved training data to " + filename);
        tempTrainingData = filename;
    }

    public Map<String, List<DataInstance>> loadModel(String filename) throws Exception {
        Map<String, List<DataInstance>> trainingData;

        if (filename == null || filename.equals("")) {
            filename = tempTrainingData;
        }

        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fis);

        trainingData = (Map<String, List<DataInstance>>) ois.readObject();
        ois.close();
        println("Loaded training data from " + filename);
        return trainingData;
    }

    public void loadClasses(String[] classNames) {
        for (String value : classNames) {
            windowMap.put(value, 0);
        }

        for (String className : classNames) {
            trainingData.put(className, new ArrayList<>());
        }
    }
}