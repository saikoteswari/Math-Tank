package nyc.tanjim.mathtank;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Random;
import java.util.Set;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;


public class QuickMath extends AppCompatActivity {
    private TextView timerText, quickMathQuestion, quickMathScore, winningMessage;
    private TextView scoreMessage, iqMessage, userFeedback;
    private Button correctButton, wrongButton;
    private int correctAnswer, score, wrongOrCorrect, numberOfQuestions, feedBackNum, musicLength;
    private Dialog scorePopUp;
    private Vibrator vibrator;
    private Animation correctAnimation;
    private Boolean addition;
    private Boolean subtraction;
    private Boolean multiplication;
    private Boolean division, kidsmode,timer, mute;
    private MediaPlayer mediaPlayer;
    private CountDownTimer countDownTimer;
    private Button playAgainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_math);

        timerText = findViewById(R.id.stopwatchText);
        quickMathQuestion = findViewById(R.id.quickMathQuestion);
        quickMathScore = findViewById(R.id.quickMathsScore);
        correctButton = findViewById(R.id.correctButton);
        wrongButton = findViewById(R.id.wrongButton);
        scorePopUp = new Dialog(this);
        scorePopUp.setContentView(R.layout.score_popup);
        scorePopUp.getWindow().getAttributes().windowAnimations = R.style.ScorePopUpAnimation;
        winningMessage = scorePopUp.findViewById(R.id.winningMessage);
        scoreMessage = scorePopUp.findViewById(R.id.scoreMessage);
        iqMessage = scorePopUp.findViewById(R.id.iqMessage);
        userFeedback = findViewById(R.id.plusOne);
        playAgainButton = findViewById(R.id.playAgainButton);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        correctAnimation = AnimationUtils.loadAnimation(this, R.anim.correct_animation);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //Gets user preferences
        addition = sharedPref.getBoolean(SettingsActivity.KEY_ADDITION_ONLY_QUICKMATH,false);
        subtraction = sharedPref.getBoolean(SettingsActivity.KEY_SUBTRACTION_ONLY_QUICKMATH,false);
        multiplication = sharedPref.getBoolean(SettingsActivity.KEY_MULTIPLICATION_ONLY_QUICKMATH,false);
        division = sharedPref.getBoolean(SettingsActivity.KEY_DIVISION_ONLY_QUICKMATH,false);
        kidsmode = sharedPref.getBoolean(SettingsActivity.KEY_KIDS_MODE_SWITCH, false);
        timer = sharedPref.getBoolean(SettingsActivity.KEY_TIMER, false);
        mute = sharedPref.getBoolean(SettingsActivity.KEY_MUTE_MUSIC,false);
//        timerDuration = sharedPref.getString(SettingsActivity.KEY_TIMER,"30");

        //Changes the background and status bar color when the timer hits 15 seconds
        generateQuestion();
        if(timer) {
            timer();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                ConstraintLayout constraintLayout = findViewById(R.id.quickMathBg);
                TransitionDrawable transitionDrawable = (TransitionDrawable) constraintLayout.getBackground();
                @Override
                public void run() {
                    transitionDrawable.startTransition(10000);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setStatusBarColor(Color.RED);
                    }
                }
            }, 15000);
        }
        else {
            Boolean darkModePref = sharedPref.getBoolean(SettingsActivity.KEY_DARK_MODE_SWITCH, false);
            if(darkModePref){
                ConstraintLayout constraintLayout = (findViewById(R.id.quickMathBg));
                constraintLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.color.qboard_black));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(getResources().getColor(R.color.qboard_black));
                }
            }
            timerText.setText(getString(R.string.taponq));
        }
        //Initialize ads
        MobileAds.initialize(this,getString(R.string.quickMathad));

        //Ad load and requests
        AdView mAdView = findViewById(R.id.qmAd);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mediaPlayer = MediaPlayer.create(this,R.raw.littleidea);
        mediaPlayer.setLooping(true);
        if(!mute) {
            mediaPlayer.start();
        }

        if(isFirstTime()){
            countDownTimer.cancel();
            timerText.setText("30s");
            ShowcaseConfig config = new ShowcaseConfig();
            config.setMaskColor(getResources().getColor(R.color.colorAccent50));
            config.setRenderOverNavigationBar(true);
            config.setDelay(500);

            MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "quickMathOnBoarding");
            sequence.setConfig(config);
            sequence.addSequenceItem(new MaterialShowcaseView.Builder(this)
                                                .setTarget(timerText)
                                                .setContentText("Try to answer as many as you can before timer runs out.")
                                                .setDismissText("Next")
                                                .setMaskColour(getResources().getColor(R.color.colorAccent50))
                                                .setDismissOnTargetTouch(true)
                                                .setTargetTouchable(true)
                                                .build()
            );
            sequence.addSequenceItem(quickMathQuestion,"Analyze the question - Don't try to MATH-it. Look for the pattern. You can click on question board to stop and restart or quit at any time.","Next");
            if(wrongOrCorrect == 0)
                sequence.addSequenceItem(
                    new MaterialShowcaseView.Builder(this)
                            .setTarget(correctButton)
                            .setContentText("I'll help you with this one - since it's a correct equation. Tap on correct.")
                            .setMaskColour(getResources().getColor(R.color.colorAccent50))
                            .setDismissOnTargetTouch(true)
                            .setTargetTouchable(true)
                            .build()
                );
            else
                sequence.addSequenceItem(
                        new MaterialShowcaseView.Builder(this)
                                .setTarget(wrongButton)
                                .setContentText("I'll help you with this one - since it's a wrong equation. Tap on wrong.")
                                .setMaskColour(getResources().getColor(R.color.colorAccent50))
                                .setDismissOnTargetTouch(true)
                                .setTargetTouchable(true)
                                .build()
                );
            sequence.addSequenceItem(userFeedback,"You will get a feedback based on your answer. Keep an eye on the background as the color changes with timer!","Next");
            sequence.addSequenceItem(
                    new MaterialShowcaseView.Builder(this)
                            .setTarget(quickMathQuestion)
                            .setContentText("Tap on question board to stop and open up the scoreboard. And you are all set!")
                            .setMaskColour(getResources().getColor(R.color.colorAccent50))
                            .setDismissOnTargetTouch(true)
                            .setTargetTouchable(true)
                            .build()
            );
            sequence.start();

        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer.isPlaying() && !mute) {
            musicLength = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(!mediaPlayer.isPlaying() && !mute) {
            mediaPlayer.seekTo(musicLength);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }
    /**
    * Method to show pop up.
     * If score difference is less than 4 and number of questions answered are greater than 10
     * then user is top tier
     * Shows an afk text if user didn't answer any questions.
     * Doesn't let user tap outside pop up box
     *
    * */
    public void showPopUp(View view){
        countDownTimer.cancel();
        if(numberOfQuestions - score < 4 && numberOfQuestions > 10){
            winningMessage.setText(getString(R.string.hey_there_genius));
        }else if(numberOfQuestions - score > 0 && numberOfQuestions - score < 5) {
            winningMessage.setText(getString(R.string.unbelievable));
        }else if(numberOfQuestions < 10 && numberOfQuestions > 1){
            winningMessage.setText(getString(R.string.are_you_even));
        }else if(numberOfQuestions == 0){
            winningMessage.setText(getString(R.string.afk_text));
        }else {
            winningMessage.setText(getString(R.string.need_more_practice));
        }
        scoreMessage.setText(getString(R.string.score_pop_score, score, numberOfQuestions));
        if(numberOfQuestions >= 10) {
            if (numberOfQuestions - score >= 0 && numberOfQuestions - score < 2) {
                iqMessage.setText(getString(R.string.exceptional_math_skill));
            } else if (numberOfQuestions - score >= 0 && numberOfQuestions - score <= 3) {
                iqMessage.setText(getString(R.string.above_average_math_skill));
            } else if (numberOfQuestions - score >= 3 && numberOfQuestions - score <= 5) {
                iqMessage.setText(getString(R.string.average_math_skill));
            } else{
                iqMessage.setText(getString(R.string.below_average_math_skill));
            }
        }else{
            iqMessage.setText(getString(R.string.number_too_low_10));
        }
        scorePopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        scorePopUp.setCanceledOnTouchOutside(false);
        if(!QuickMath.this.isFinishing()) {
            scorePopUp.show();
        }
        scorePopUp.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }

    public void showPopUp(){
        if(numberOfQuestions - score < 4 && numberOfQuestions > 10){
            winningMessage.setText(getString(R.string.hey_there_genius));
        }else if(numberOfQuestions - score > 0 && numberOfQuestions - score < 5) {
            winningMessage.setText(getString(R.string.unbelievable));
        }else if(numberOfQuestions < 10 && numberOfQuestions > 1){
            winningMessage.setText(getString(R.string.are_you_even));
        }else if(numberOfQuestions == 0){
            winningMessage.setText(getString(R.string.afk_text));
        }else {
            winningMessage.setText(getString(R.string.need_more_practice));
        }
        scoreMessage.setText(getString(R.string.score_pop_score, score, numberOfQuestions));
        if(numberOfQuestions > 20) {
            if (numberOfQuestions - score >= 0 && numberOfQuestions - score <= 5) {
                iqMessage.setText(getString(R.string.exceptional_math_skill));
            } else if (numberOfQuestions - score >= 6 && numberOfQuestions - score <= 10) {
                iqMessage.setText(getString(R.string.above_average_math_skill));
            } else if (numberOfQuestions - score >= 11 && numberOfQuestions - score <= 15) {
                iqMessage.setText(getString(R.string.average_math_skill));
            }else{
                iqMessage.setText(getString(R.string.below_average_math_skill));
            }
        }else {
            iqMessage.setText(getString(R.string.number_too_low));
        }
        scorePopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        scorePopUp.setCanceledOnTouchOutside(false);
        if(!QuickMath.this.isFinishing()) {
            scorePopUp.show();
        }
        scorePopUp.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }

    //Pop up Play again button
    public void playAgain(View view){
        finish();
        startActivity(new Intent(getApplicationContext(), QuickMathLoadingScreen.class));
    }

    //Pop up quit button
    public  void quit(View view){
        finish();
    }


    /**The answer button
     * if user answers correct score increases by one
     * number of questions increases by one
     * score text updates
     * Feeds feedback to user depending on right or wrong answer
     * */
    public void choose(View view){
        userFeedback.startAnimation(AnimationUtils.loadAnimation(this,R.anim.flicker_animation));
        if(view.getTag().toString().equals(Integer.toString(wrongOrCorrect))){
            score++;
            generateQuestion();
            quickMathQuestion.startAnimation(AnimationUtils.loadAnimation(this,R.anim.question_flicker));
            quickMathScore.setText(getString(R.string.score,score));
            numberOfQuestions++;
            if(feedBackNum == 0 || numberOfQuestions == 1){
                userFeedback.setText(getString(R.string.good_job));
            }else if(feedBackNum == 1){
                userFeedback.setText(getString(R.string.amazing));
            }else if(feedBackNum == 2){
                userFeedback.setText(getString(R.string.fantastic));
            }else if(feedBackNum == 3){
                userFeedback.setText(getString(R.string.damn));
            }else if(feedBackNum == 4){
                userFeedback.setText(getString(R.string.genius));
            }else if(feedBackNum == 5){
                userFeedback.setText(getString(R.string.sweet));
            }else if(feedBackNum == 6){
                userFeedback.setText(getString(R.string.crazy));
            }else if(feedBackNum == 7){
                userFeedback.setText(getString(R.string.keep));
            }else if(feedBackNum == 8){
                userFeedback.setText(getString(R.string.unbelievable));
            }else if(feedBackNum == 9){
                userFeedback.setText(getString(R.string.surprised));
            }else if(feedBackNum == 10){
                userFeedback.setText(getString(R.string.brilliant));
            }else if(feedBackNum == 11){
                userFeedback.setText(getString(R.string.bananas));
            }
        }else{
            quickMathScore.startAnimation(AnimationUtils.loadAnimation(this,R.anim.correct_animation));
            vibrator.vibrate(500);
            if(wrongOrCorrect == 1){
                wrongButton.startAnimation(correctAnimation);
            }else{
                correctButton.startAnimation(correctAnimation);
            }
            Random rd = new Random();
            switch (rd.nextInt(3)) {
                case 0:
                    userFeedback.setText(getString(R.string.ohno));
                    break;
                case 1:
                    userFeedback.setText(getString(R.string.next));
                    break;
                case 2:
                    userFeedback.setText(getString(R.string.sad));
                    break;
            }
            generateQuestion();
            numberOfQuestions++;

        }
    }

    //Generates write or wrong question randomly depending on user preference
    public void generateQuestion(){
        Random rd = new Random();
        int questionType = rd.nextInt(4);
        feedBackNum = rd.nextInt(12);
        if(questionType == 0){
            if(addition) {
                sumQuestion();
            }else{
                if(subtraction) {
                    subtractQuestion();
                }else{
                    if(multiplication)
                        multiplyQuestions();
                    else{
                        if(division)
                            divisionQuestion();
                    }
            }
            }
        }else if(questionType == 1){
            if(subtraction) {
                subtractQuestion();
            }else{
                if(addition) {
                    sumQuestion();
                }else{
                    if(multiplication)
                        multiplyQuestions();
                    else{
                        if(division)
                            divisionQuestion();
                    }
                }
            }

        }else if(questionType == 2){
            if(multiplication) {
                multiplyQuestions();
            }else{
                if(addition) {
                    sumQuestion();
                }else{
                    if(subtraction)
                        subtractQuestion();
                    else{
                        if(division)
                            divisionQuestion();
                    }
                }
            }
        }else if(questionType == 3){
            if(division) {
                divisionQuestion();
            }else{
                if(addition) {
                    sumQuestion();
                }else{
                    if(subtraction)
                        subtractQuestion();
                    else{
                        if(multiplication)
                            multiplyQuestions();
                    }
                }
            }
        }
    }
    //Creates a sum questions
    public void sumQuestion(){
        Random rd = new Random();
        int a,b;
        if(!kidsmode) {
            a = rd.nextInt((25 - 10) + 1) + 10;
            b = rd.nextInt((25 - 10) + 1) + 10;
        }else {
            a = rd.nextInt((12 - 1) + 1) + 1;
            b = rd.nextInt((12 - 1) + 1) + 1;
        }
        wrongOrCorrect = rd.nextInt(2);
        int incorrectAnswer;
        if(wrongOrCorrect == 0){
            correctAnswer = a + b;
            quickMathQuestion.setText(getString(R.string.sum,a,b,correctAnswer));
        }else{
            incorrectAnswer = rd.nextInt((50-12)+1)+12;
            while(incorrectAnswer == correctAnswer){
                incorrectAnswer = rd.nextInt((50-12)+1)+12;
            }
            quickMathQuestion.setText(getString(R.string.sum,a,b,incorrectAnswer));
        }
    }

    //Creates a subtract question
    public void subtractQuestion(){
        Random rd = new Random();
        int c,d;
        if(!kidsmode) {
            c = rd.nextInt(25) + 1;
            d = rd.nextInt(10) + c;
        }else{
            c = rd.nextInt(12) + 1;
            d = rd.nextInt(1) + c;
        }
        int incorrectAnswer;
        wrongOrCorrect = rd.nextInt(2);
        if(wrongOrCorrect == 0 ) {
            quickMathQuestion.setText(getString(R.string.sub,d,c,d-c));
        }else{
            correctAnswer = d - c;
            incorrectAnswer = rd.nextInt(20)+1;
            while(incorrectAnswer == correctAnswer){
                incorrectAnswer = rd.nextInt(20)+1;
            }
            quickMathQuestion.setText(getString(R.string.sub,d,c,incorrectAnswer));
        }
    }

    //Creates a multiply question
    public void multiplyQuestions(){
        Random rd = new Random();
        int a,b;
        if(!kidsmode) {
            a = rd.nextInt((12 - 1) + 1) + 1;
            b = rd.nextInt((12 - 1) + 1) + 1;
        }else{
            a = rd.nextInt((9 - 1) + 1) + 1;
            b = rd.nextInt((9 - 1) + 1) + 1;
        }
        correctAnswer = a * b;
        wrongOrCorrect = rd.nextInt(2);
        int incorrectAnswer;
        if(wrongOrCorrect == 0){
            quickMathQuestion.setText(getString(R.string.mult,a,b,correctAnswer));
        }else{
            incorrectAnswer = rd.nextInt((100-20)+1)+20;
            while(incorrectAnswer == correctAnswer){
                incorrectAnswer = rd.nextInt((100-20)+1)+20;
            }
            quickMathQuestion.setText(getString(R.string.mult,a,b,incorrectAnswer));
        }
    }

    //Creates a division question
    public void divisionQuestion(){
        Random rd = new Random();
        int a,b;
        if(!kidsmode) {
            a = rd.nextInt((25 - 10) + 1) + 10;
            b = rd.nextInt((25 - 10) + 1) + 10;
        }else{
            a = rd.nextInt((12 - 1) + 1) + 1;
            b = rd.nextInt((12 - 1) + 1) + 1;
        }
        int incorrectAnswer;
        wrongOrCorrect = rd.nextInt(2);
        if(wrongOrCorrect == 0){
            while(b % a != 0){
                a = rd.nextInt(10)+1;
                b = rd.nextInt(10)+a;
            }
            correctAnswer = b / a;
            quickMathQuestion.setText(getString(R.string.div,b,a,correctAnswer));
        }else{
            incorrectAnswer = rd.nextInt(24)+1;
            correctAnswer = b / a;
            while(b % a != 0 || incorrectAnswer == correctAnswer){
                a = rd.nextInt(10)+1;
                b = rd.nextInt(10)+a;
            }
            quickMathQuestion.setText(getString(R.string.div,b,a,incorrectAnswer));
        }
    }

    //Timer that keeps track of time
    public void timer(){
//        int time = Integer.parseInt(timerDuration) * 1000;
        countDownTimer = new CountDownTimer(30000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                if(millisUntilFinished > 10000)
                    timerText.setText(getString(R.string.timer_quick_math,(int) millisUntilFinished / 1000));
                else if(millisUntilFinished < 10000 && millisUntilFinished > 5000){
                    //Timer flickering gets faster as time runs out
                    //Adds a 0 before last digit
                    timerText.setText(getString(R.string.timer_quick_math_ten_less,(int) millisUntilFinished / 1000));
                    timerText.startAnimation(AnimationUtils.loadAnimation(QuickMath.this, R.anim.flicker_animation_2));
                }else if(millisUntilFinished < 5000 && millisUntilFinished > 3000){
                    timerText.setText(getString(R.string.timer_quick_math_ten_less,(int) millisUntilFinished / 1000));
                    timerText.startAnimation(AnimationUtils.loadAnimation(QuickMath.this, R.anim.flicker_animation_1));
                }else {
                    timerText.setText(getString(R.string.timer_quick_math_ten_less,(int) millisUntilFinished / 1000));
                    timerText.startAnimation(AnimationUtils.loadAnimation(QuickMath.this, R.anim.flicker_animation));
                }
            }
            @Override
            public void onFinish() {
                //Shows user results
               showPopUp();
            }
        }.start();
    }

    //Resets everything to zero
    public void reset(){
        numberOfQuestions = 0;
        score = 0;
        timer();
        generateQuestion();
    }

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.commit();
        }
        return !ranBefore;
    }

}
