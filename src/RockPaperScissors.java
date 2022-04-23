import processing.core.PApplet;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/* A class with the main function and Processing visualizations to run the demo */

public class RockPaperScissors extends ClassifyVibration {

    @Override
    public String[] setClassNames() {
        return new String[]{"quiet", "rock", "paper", "scissors"};
    }

    public static void main(String[] args) {
        PApplet.main("RockPaperScissors");
    }

    // TODO add state machine for p1->p2->result->reset and announce each step

    @Override
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
            if (prevOutput != "" && currCountDisplay < DISPLAY_LENGTH) {
                if (currCountDisplay == 0) {
                    playSounds(Arrays.asList("p1", "chooses", prevOutput));
                }
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
                    if (output != "quiet") {
                        prevOutput = output;
                    }
                    println(windowMap);
                }

                text("classified as: " + output, 20, 30);
            }
        } else {
            text(classNames[classIndex], 20, 30);
            dataCount = trainingData.get(classNames[classIndex]).size();
            text("Data collected: " + dataCount, 20, 60);
        }
    }


    private static void playSounds(List<String> sounds) {
        new Thread(() -> {
            sounds.forEach(RockPaperScissors::playSound);
        }).start();
    }

    private static void playSound(String filename) {
        File f = new File("./sounds/" + filename + ".wav");
        AudioInputStream audioIn = null;
        try {
            audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            while (clip.getMicrosecondLength() != clip.getMicrosecondPosition()) {
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}