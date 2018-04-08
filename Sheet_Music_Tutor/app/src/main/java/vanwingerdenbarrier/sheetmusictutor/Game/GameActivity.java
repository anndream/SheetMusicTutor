package vanwingerdenbarrier.sheetmusictutor.Game;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;

import java.time.Instant;
import java.util.Random;

import vanwingerdenbarrier.sheetmusictutor.Key.KeyFragment;
import vanwingerdenbarrier.sheetmusictutor.NoteGames.GuessNote;
import vanwingerdenbarrier.sheetmusictutor.NoteGames.GuessNoteText;
import vanwingerdenbarrier.sheetmusictutor.NoteGames.NoteDefense;
import vanwingerdenbarrier.sheetmusictutor.NoteGames.NoteHero;
import vanwingerdenbarrier.sheetmusictutor.Quiz.QuizAnswerFragment;
import vanwingerdenbarrier.sheetmusictutor.Quiz.QuizQuestionFragment;
import vanwingerdenbarrier.sheetmusictutor.R;
import vanwingerdenbarrier.sheetmusictutor.ResultsActivity;
import vanwingerdenbarrier.sheetmusictutor.StaffStructure.Note;
import vanwingerdenbarrier.sheetmusictutor.StaffStructure.StaffFragment;
import vanwingerdenbarrier.sheetmusictutor.UserInfo.User;
import vanwingerdenbarrier.sheetmusictutor.UserInfo.UserList;

/**
 * @author Bronson VanWingerden
 * the game activity screen to display the Staff Fragment and the Key Fragment
 */
public class GameActivity extends FragmentActivity
        implements QuestionDisplay.Display, AnswerDisplay.Display {

    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;
    Fragment currentQuestion;
    Fragment currentAnswer;

    /**
     * indicates the current game mode
     * 0 = quiz,
     * 1 = staff,
     * 2 = combo
     * etc
     */
    int mode;

    /**
     * rounds
     */

    int rounds;

    /**
     * Allows us to pass information between our fragments if object is null then the question is done
     * and the score and remaining lives are passed via score and lives if applicable
     */
    public void questionPressed(Object correct, int score, int lives) {

        if(correct == null){
            endQuestion(score, lives);
        }else if(currentAnswer instanceof QuizAnswerFragment){
            ((QuizAnswerFragment) currentAnswer).setQuestion((int)correct);
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.answer_holder, currentAnswer);
            fragmentTransaction.commit();
        }

    }

    /**
     * Allows us to pass info between fragments
     * @param answer
     * @param event
     */
    public void answerPressed(Object answer, MotionEvent event) {
        if (currentQuestion instanceof StaffFragment && event != null) {
            ((StaffFragment) currentQuestion)
                    .colorNoteOnStaff(((StaffFragment) currentQuestion)
                            .getNoteAtCurrentLocation((Note) answer), event);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                new UserList(this.getApplicationContext()).addUserAttempt();
            }
        }else if(currentQuestion instanceof QuizQuestionFragment){

            ((QuizQuestionFragment) currentQuestion).checkIfCorrect((String)answer);
            new UserList(this.getApplicationContext()).addUserAttempt();
        } else if (currentQuestion instanceof NoteDefense && event != null) {
            ((NoteDefense) currentQuestion).fireNote((Note) answer);
        } else if (currentQuestion instanceof NoteHero && event != null) {
            ((NoteHero) currentQuestion).playNote((Note) answer);
        }else if (currentQuestion instanceof GuessNote){

            ((GuessNote) currentQuestion).checkIfCorrect((String)answer);
            new UserList(this.getApplicationContext()).addUserAttempt();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);//set media volume control

        rounds = 6;

        int gameType = getIntent().getIntExtra("gameType", -1);

        mode = gameType;

        fragmentManager = getSupportFragmentManager();

        if (gameType == 1 || gameType == 2) {
            addQuestion(new StaffFragment());
            replaceAnswer(setFragmentArgs(new KeyFragment(), 0));
        } else if (gameType == 3) {
            addQuestion(new NoteDefense());
            replaceAnswer(setFragmentArgs(new KeyFragment(), 1));
        } else if (gameType == 4) {
            addQuestion(new NoteHero());
            replaceAnswer(setFragmentArgs(new KeyFragment(), 0));
        } else if (gameType == 5) {
            addQuestion(new GuessNote());
            addAnswer(new GuessNoteText());
        } else {
            System.out.println("AAA GAMETYPE = NOTFOUND" + gameType);
        }


        setContentView(R.layout.activity_game);
    }

    /**
     * adds the passed fragment to the current question holder
     * @param fragment the fragment to add
     */
    public void addQuestion(Fragment fragment) {
        currentQuestion = fragment;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.question_holder, fragment);
        fragmentTransaction.commit();

    }

    /**
     * adds the passed fragment to the current answer holder
     * @param fragment the fragment to add
     */
    public void addAnswer(Fragment fragment) {
        currentAnswer = fragment;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.answer_holder, fragment);
        fragmentTransaction.commit();
    }

    /**
     * ends the current question
     */
    public void endQuestion(int score, int lives) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        if(lives > 0) {
            alertDialog.setTitle("Good Job!");
            alertDialog.setMessage("You scored:" + score + "!");

        }else{
            alertDialog.setTitle("Too Bad!");
            alertDialog.setMessage("You ran out of lives!!");
            alertDialog.setMessage("You scored:" + score + "!");
        }

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {

                        if (rounds <= 1) {
                            //finish();
                            sendResults();
                        } else {
                            makeNextQuestion();
                        }

                        dialogInterface.dismiss();
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    /**
     * Once quiz completes send results to results screen
     */
    public void sendResults(){

        User current = new UserList(getBaseContext()).findCurrent();

        boolean isQuiz = true;

        float percentage = ( (float) current.getNumQuestionsCorrect()/ (float) current.getNumPointsNeeded())*100;

        Intent stats = new Intent(this, ResultsActivity.class);

        stats.putExtra("percent",(int) percentage);//random number for now(Level progress)
        stats.putExtra("correct",current.getNumQuestionsCorrect());
        stats.putExtra("numQuestions",current.getNumQuestionsAttempted());
        stats.putExtra("score",current.getCurrentLevel());//random number for now(Score)
        stats.putExtra("points",current.getNumPointsNeeded());
        stats.putExtra("isQuiz",isQuiz);

        this.startActivity(stats);
    }//end sendResults

    /**
     * replaces the question with a new question fragment
     * @param fragment
     */
    public void replaceQuestion(Fragment fragment){
        currentQuestion = fragment;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.question_holder, fragment);
        fragmentTransaction.commit();
    }

    /**
     * replaces the answer witha new question fragment
     * @param fragment
     */
    public void replaceAnswer(Fragment fragment){
        currentAnswer = fragment;
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.answer_holder, fragment);
        fragmentTransaction.commit();
    }

    /**
     * creates the next question
     */
    public void makeNextQuestion(){
        if (mode == 1) {
            replaceQuestion(new StaffFragment());
            rounds--;

        }else if (mode == 2){

            Random rand = new Random();
            int next = rand.nextInt(4);

            if(next == 0){
                replaceQuestion(new StaffFragment());
                replaceAnswer(setFragmentArgs(new KeyFragment(), 0));
            }else if(next == 1){
                replaceQuestion(new QuizQuestionFragment());
                replaceAnswer(new QuizAnswerFragment());
            }else if(next == 2){
                replaceQuestion(new NoteDefense());
                replaceAnswer(setFragmentArgs(new KeyFragment(), 1));
            }else if(next == 3){
                replaceQuestion(new NoteHero());
                replaceAnswer(setFragmentArgs(new KeyFragment(), 0));
            }
            rounds--;

        } else if (mode == 3) {
            replaceQuestion(new NoteDefense());
            rounds--;
        } else if (mode == 4) {
            replaceQuestion(new NoteHero());
            rounds--;
        }
    }

    public Fragment setFragmentArgs(Fragment fragment, int mode){
        Bundle args = new Bundle();
        args.putInt("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

}

