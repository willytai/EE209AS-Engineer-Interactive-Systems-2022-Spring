import processing.core.PApplet;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* A class with the main function and Processing visualizations to run the demo */

public class RockPaperScissors extends ClassifyVibration {

    @Override
    public String[] setClassNames() {
        return new String[]{"quiet", "rock", "paper", "scissors"};
    }

    @Override
    public String setTrainingDataFilename() { // Uses tempTrainingData if not defined
        return "";
//        return "./trainingData/RockPaperScissors_2022_04_23_16_23_09.trainingdata";
    }

    public static void main(String[] args) {
        PApplet.main("RockPaperScissors");
    }

    String playerOne = null;
    String playerTwo = null;
    boolean init = true;

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
            if (init) {
                playSounds(Arrays.asList("p1", "yourTurn"));
                init = false;
            }

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

                if (output != "quiet") {
                    if (playerOne == null) {
                        playerOne = output;
                        playSounds(Arrays.asList("p2", "yourTurn"));
                    } else {
                        playerTwo = output;
                        announceResult();
                    }
                    windowList.clear();
                    for (String value : classNames) {
                        windowMap.put(value, 0);
                    }
                }
                println(windowMap, playerOne, playerTwo);
            }

            text("classified as: " + output, 20, 30);
        } else {
            text(classNames[classIndex], 20, 30);
            dataCount = trainingData.get(classNames[classIndex]).size();
            text("Data collected: " + dataCount, 20, 60);
        }
    }

    private void announceResult() {
        List<String> output = new ArrayList<>();
        output.add("p1");
        output.add("chooses");
        output.add(playerOne);
        output.add("p2");
        output.add("chooses");
        output.add(playerTwo);

        if ((playerOne.equals("rock") && playerTwo.equals("scissors")) || (playerOne.equals("paper") && playerTwo.equals("rock")) || (playerOne.equals("scissors") && playerTwo.equals("paper"))) {
            output.add("p1");
            output.add("wins");
            output.add("congrats");
        } else if ((playerTwo.equals("rock") && playerOne.equals("scissors")) || (playerTwo.equals("paper") && playerOne.equals("rock")) || (playerTwo.equals("scissors") && playerOne.equals("paper"))) {
            output.add("p2");
            output.add("wins");
            output.add("congrats");
        } else {
            output.add("itsATiePlayAgain");
        }
        playSounds(output);
        init = true;
        playerOne = null;
        playerTwo = null;
    }

    private static void playSounds(List<String> sounds) {
        // Use separate thread if we want non blocking
        // in this case we dont want to listen while playing audio
//        new Thread(() -> {
//        }).start();
        sounds.forEach(RockPaperScissors::playSound);
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