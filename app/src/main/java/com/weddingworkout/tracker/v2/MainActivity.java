package com.weddingworkout.tracker.v2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "workout_v2";
    private static final String SESSION_PREFIX = "session_";
    private static final String LEGACY_WEEK_PREFIX = "legacy_week_";
    private static final String LEGACY_LAST_PREFIX = "legacy_last_";
    private static final String LEGACY_UNIT = "legacy_unit";
    private static final String MIGRATION_CHECKED = "legacy_auto_migration_checked_v22";
    private static final int WEEKS_PER_PAGE = 10;
    private static final int PICK_LEGACY_XML = 1201;
    private static final String LEGACY_IMPORT_COMPLETE = "legacy_import_complete_v222";

    private static final int BG = Color.rgb(30, 31, 34);
    private static final int SURFACE = Color.rgb(43, 45, 49);
    private static final int SURFACE_2 = Color.rgb(50, 52, 58);
    private static final int BORDER = Color.rgb(66, 69, 76);
    private static final int BLUE = Color.rgb(59, 130, 246);
    private static final int TEXT = Color.rgb(245, 247, 250);
    private static final int MUTED = Color.rgb(183, 187, 196);

    private final DateTimeFormatter displayDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.US);
    private final DateTimeFormatter dayDate = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US);

    private SharedPreferences prefs;
    private LinearLayout content;
    private final Map<DayOfWeek, DayPlan> plans = new LinkedHashMap<>();
    private final List<ExerciseInput> activeInputs = new ArrayList<>();
    private String unit = "lb";

    private Screen screen = Screen.TODAY;
    private int savedPage = 0;
    private LocalDate openWeek = null;

    private enum Screen {
        TODAY,
        SAVED,
        WEEK,
        IMPORT
    }

    static class ExercisePlan {
        final String block;
        final String id;
        final String name;
        final int sets;
        final int repMin;
        final int repMax;

        ExercisePlan(String block, String id, String name, int sets, int repMin, int repMax) {
            this.block = block;
            this.id = id;
            this.name = name;
            this.sets = sets;
            this.repMin = repMin;
            this.repMax = repMax;
        }
    }

    static class DayPlan {
        final DayOfWeek day;
        final String title;
        final String subtitle;
        final ExercisePlan[] exercises;

        DayPlan(DayOfWeek day, String title, String subtitle, ExercisePlan... exercises) {
            this.day = day;
            this.title = title;
            this.subtitle = subtitle;
            this.exercises = exercises;
        }
    }

    static class SetInput {
        final EditText weight;
        final EditText reps;

        SetInput(EditText weight, EditText reps) {
            this.weight = weight;
            this.reps = reps;
        }
    }

    static class ExerciseInput {
        final ExercisePlan plan;
        final List<SetInput> sets = new ArrayList<>();

        ExerciseInput(ExercisePlan plan) {
            this.plan = plan;
        }
    }

    static class PreviousExercise {
        final LocalDate date;
        final JSONArray sets;
        final String unit;

        PreviousExercise(LocalDate date, JSONArray sets, String unit) {
            this.date = date;
            this.sets = sets;
            this.unit = unit;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        unit = prefs.getString("unit", "lb");
        initPlans();
        attemptAutomaticLegacyMigration();
        showToday();
    }

    private void initPlans() {
        plans.clear();

        plans.put(DayOfWeek.MONDAY, new DayPlan(
                DayOfWeek.MONDAY,
                "Push",
                "Chest, shoulders, triceps • 16 working sets",
                new ExercisePlan("Superset A", "db_bench", "Dumbbell Bench Press", 4, 6, 10),
                new ExercisePlan("Superset A", "db_lateral_raise", "Dumbbell Lateral Raise", 3, 12, 20),
                new ExercisePlan("Superset B", "incline_db_press", "Incline Dumbbell Press", 3, 8, 12),
                new ExercisePlan("Superset B", "db_triceps_ext", "Dumbbell Overhead Triceps Extension", 3, 10, 15),
                new ExercisePlan("Block C", "db_ohp", "Dumbbell Overhead Press", 3, 8, 12)
        ));

        plans.put(DayOfWeek.TUESDAY, new DayPlan(
                DayOfWeek.TUESDAY,
                "Pull",
                "Back, rear delts, biceps • 16 working sets",
                new ExercisePlan("Superset A", "db_row", "One-Arm Dumbbell Row", 4, 8, 12),
                new ExercisePlan("Superset A", "hammer_curl", "Hammer Curl", 3, 8, 15),
                new ExercisePlan("Superset B", "chest_supported_db_row", "Chest-Supported Dumbbell Row", 3, 8, 12),
                new ExercisePlan("Superset B", "rear_delt_db_fly", "Dumbbell Rear-Delt Fly", 3, 12, 20),
                new ExercisePlan("Block C", "db_pullover", "Dumbbell Pullover", 3, 10, 15)
        ));

        plans.put(DayOfWeek.WEDNESDAY, new DayPlan(
                DayOfWeek.WEDNESDAY,
                "Legs + Core",
                "Lower body and core • 17 working sets",
                new ExercisePlan("Superset A", "db_rdl", "Dumbbell Romanian Deadlift", 4, 8, 12),
                new ExercisePlan("Superset A", "db_calf_raise", "Standing Dumbbell Calf Raise", 4, 12, 20),
                new ExercisePlan("Superset B", "goblet_squat", "Dumbbell Goblet Squat", 3, 8, 12),
                new ExercisePlan("Superset B", "weighted_crunch", "Weighted Dumbbell Crunch", 3, 10, 20),
                new ExercisePlan("Block C", "db_split_squat", "Dumbbell Split Squat", 3, 8, 12)
        ));

        plans.put(DayOfWeek.THURSDAY, new DayPlan(
                DayOfWeek.THURSDAY,
                "Upper",
                "Chest, back, side and rear delts • 18 working sets",
                new ExercisePlan("Superset A", "incline_db_press", "Incline Dumbbell Press", 3, 8, 12),
                new ExercisePlan("Superset A", "db_row", "One-Arm Dumbbell Row", 3, 8, 12),
                new ExercisePlan("Superset B", "db_bench", "Dumbbell Bench Press", 3, 8, 12),
                new ExercisePlan("Superset B", "db_pullover", "Dumbbell Pullover", 3, 10, 15),
                new ExercisePlan("Superset C", "db_lateral_raise", "Dumbbell Lateral Raise", 3, 12, 20),
                new ExercisePlan("Superset C", "rear_delt_db_fly", "Dumbbell Rear-Delt Fly", 3, 12, 20)
        ));

        plans.put(DayOfWeek.FRIDAY, new DayPlan(
                DayOfWeek.FRIDAY,
                "Shoulders + Arms + Glutes",
                "Shoulders, arms, glutes • 18 working sets",
                new ExercisePlan("Superset A", "db_ohp", "Dumbbell Overhead Press", 3, 8, 12),
                new ExercisePlan("Superset A", "db_curl", "Dumbbell Curl", 3, 8, 15),
                new ExercisePlan("Superset B", "db_lateral_raise", "Dumbbell Lateral Raise", 3, 12, 20),
                new ExercisePlan("Superset B", "db_triceps_ext", "Dumbbell Overhead Triceps Extension", 3, 10, 15),
                new ExercisePlan("Superset C", "hammer_curl", "Hammer Curl", 3, 10, 15),
                new ExercisePlan("Superset C", "db_hip_thrust", "Dumbbell Hip Thrust / Glute Bridge", 3, 10, 15)
        ));
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void base(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        TextView h = text(title, 27, TEXT);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.setPadding(0, 0, 0, dp(4));
        content.addView(h);
    }

    private TextView text(String value, int sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(0, 1.08f);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView muted(String value, int sp) {
        return text(value, sp, MUTED);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        return bg;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(52));
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setBackground(rounded(BLUE, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private Button compactButton(String label) {
        Button b = button(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1);
        b.setLayoutParams(lp);
        return b;
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.30f);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(13), dp(14), dp(14));
        GradientDrawable bg = rounded(SURFACE, 14);
        bg.setStroke(dp(1), BORDER);
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(11);
        c.setLayoutParams(lp);
        return c;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return v;
    }

    private void showToday() {
        screen = Screen.TODAY;
        openWeek = null;
        activeInputs.clear();

        LocalDate today = LocalDate.now();
        DayPlan plan = plans.get(today.getDayOfWeek());

        base("Today's Workout");
        TextView date = text(today.format(dayDate), 16, MUTED);
        content.addView(date);

        if (plan == null) {
            LinearLayout c = card();
            TextView rest = text("Rest Day", 22, TEXT);
            rest.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            c.addView(rest);
            c.addView(muted("No programmed workout today.", 15));
            content.addView(c);
        } else {
            TextView title = text(plan.title, 22, TEXT);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setPadding(0, dp(8), 0, 0);
            content.addView(title);
            content.addView(muted(plan.subtitle, 14));

            JSONObject current = readSession(today);
            String lastBlock = "";

            for (ExercisePlan ex : plan.exercises) {
                if (!ex.block.equals(lastBlock)) {
                    TextView block = text(ex.block, 15, Color.rgb(147, 197, 253));
                    block.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    block.setPadding(0, dp(18), 0, dp(2));
                    content.addView(block);
                    lastBlock = ex.block;
                }
                addExerciseCard(today, ex, current);
            }

            Button save = button(current == null ? "Save Workout" : "Update Today's Workout");
            save.setOnClickListener(v -> saveSession(today, plan));
            content.addView(save);
        }

        content.addView(spacer(18));
        Button saved = button("Saved Workouts");
        saved.setOnClickListener(v -> {
            savedPage = 0;
            showSaved();
        });
        content.addView(saved);
    }

    private void addExerciseCard(LocalDate date, ExercisePlan ex, JSONObject currentSession) {
        LinearLayout c = card();

        TextView name = text(ex.name, 18, TEXT);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(name);
        c.addView(muted(ex.sets + " sets × " + ex.repMin + "–" + ex.repMax + " reps", 13));

        PreviousExercise previous = findPreviousExercise(ex.id, date);
        if (previous != null) {
            c.addView(text("Previous: " + summarizeSets(previous.sets, previous.unit), 13, Color.rgb(147, 197, 253)));
            if (allSetsAtTop(previous.sets, ex.repMax)) {
                TextView cue = text("Increase weight next time", 12, Color.rgb(147, 197, 253));
                cue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                c.addView(cue);
            }
        } else {
            String legacy = prefs.getString(LEGACY_LAST_PREFIX + ex.id, "");
            if (!legacy.isEmpty()) {
                String legacyUnit = prefs.getString(LEGACY_UNIT, unit);
                c.addView(text("Previous: " + legacy + " " + legacyUnit, 13, Color.rgb(147, 197, 253)));
            }
        }

        JSONArray existingSets = getExerciseSets(currentSession, ex.id);
        ExerciseInput input = new ExerciseInput(ex);

        for (int i = 0; i < ex.sets; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, 0);

            TextView setLabel = text(String.valueOf(i + 1), 14, MUTED);
            setLabel.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(setLabel, new LinearLayout.LayoutParams(dp(30), dp(50)));

            EditText weight = numberField(unit);
            EditText reps = integerField("reps");

            String[] values = existingValues(existingSets, previous, ex, i);
            if (!values[0].isEmpty()) weight.setText(values[0]);
            if (!values[1].isEmpty()) reps.setText(values[1]);

            row.addView(weight, new LinearLayout.LayoutParams(0, dp(50), 1));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(50), 1);
            rp.leftMargin = dp(8);
            row.addView(reps, rp);

            input.sets.add(new SetInput(weight, reps));
            c.addView(row);
        }

        activeInputs.add(input);
        content.addView(c);
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(138, 143, 153));
        e.setInputType(inputType);
        e.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable bg = rounded(SURFACE_2, 10);
        bg.setStroke(dp(1), BORDER);
        e.setBackground(bg);
        return e;
    }

    private EditText numberField(String hint) {
        return field(hint, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }

    private EditText integerField(String hint) {
        return field(hint, InputType.TYPE_CLASS_NUMBER);
    }

    private String[] existingValues(JSONArray existing, PreviousExercise previous, ExercisePlan ex, int index) {
        String weight = "";
        String reps = "";
        try {
            if (existing != null && index < existing.length()) {
                JSONObject s = existing.optJSONObject(index);
                if (s != null) {
                    weight = s.optString("weight", "");
                    reps = s.optString("reps", "");
                    return new String[]{weight, reps};
                }
            }

            if (previous != null && index < previous.sets.length()) {
                JSONObject s = previous.sets.optJSONObject(index);
                if (s != null) weight = s.optString("weight", "");
            }

            if (weight.isEmpty()) {
                String legacy = prefs.getString(LEGACY_LAST_PREFIX + ex.id, "");
                if (!legacy.isEmpty()) weight = legacy;
            }
        } catch (Exception ignored) {
        }
        return new String[]{weight, reps};
    }

    private JSONObject readSession(LocalDate date) {
        String raw = prefs.getString(SESSION_PREFIX + date, "");
        if (raw.isEmpty()) return null;
        try {
            return new JSONObject(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONArray getExerciseSets(JSONObject session, String id) {
        if (session == null) return null;
        JSONArray exercises = session.optJSONArray("exercises");
        if (exercises == null) return null;
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject e = exercises.optJSONObject(i);
            if (e != null && id.equals(e.optString("id"))) return e.optJSONArray("sets");
        }
        return null;
    }

    private void saveSession(LocalDate date, DayPlan plan) {
        try {
            JSONObject session = new JSONObject();
            session.put("date", date.toString());
            session.put("day", date.getDayOfWeek().name());
            session.put("title", plan.title);
            session.put("unit", unit);
            session.put("savedAtEpochMs", System.currentTimeMillis());

            JSONArray exercises = new JSONArray();
            for (ExerciseInput input : activeInputs) {
                JSONObject e = new JSONObject();
                e.put("id", input.plan.id);
                e.put("name", input.plan.name);
                e.put("block", input.plan.block);
                e.put("repMin", input.plan.repMin);
                e.put("repMax", input.plan.repMax);

                JSONArray sets = new JSONArray();
                for (SetInput s : input.sets) {
                    JSONObject set = new JSONObject();
                    set.put("weight", s.weight.getText().toString().trim());
                    set.put("reps", s.reps.getText().toString().trim());
                    sets.put(set);
                }
                e.put("sets", sets);
                exercises.put(e);
            }
            session.put("exercises", exercises);

            // Each calendar day has its own key. Saving today cannot overwrite another date.
            prefs.edit().putString(SESSION_PREFIX + date, session.toString()).apply();
            Toast.makeText(this, "Workout saved", Toast.LENGTH_SHORT).show();
            showToday();
        } catch (Exception ignored) {
            Toast.makeText(this, "Unable to save workout", Toast.LENGTH_SHORT).show();
        }
    }

    private PreviousExercise findPreviousExercise(String id, LocalDate beforeDate) {
        List<LocalDate> dates = sessionDatesBefore(beforeDate);

        for (LocalDate d : dates) {
            if (d.getDayOfWeek() != beforeDate.getDayOfWeek()) continue;
            JSONObject session = readSession(d);
            JSONArray sets = getExerciseSets(session, id);
            if (hasAnyWeight(sets)) return new PreviousExercise(d, sets, session == null ? unit : session.optString("unit", unit));
        }

        for (LocalDate d : dates) {
            JSONObject session = readSession(d);
            JSONArray sets = getExerciseSets(session, id);
            if (hasAnyWeight(sets)) return new PreviousExercise(d, sets, session == null ? unit : session.optString("unit", unit));
        }
        return null;
    }

    private List<LocalDate> sessionDatesBefore(LocalDate beforeDate) {
        List<LocalDate> dates = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(SESSION_PREFIX)) continue;
            try {
                LocalDate d = LocalDate.parse(key.substring(SESSION_PREFIX.length()));
                if (d.isBefore(beforeDate)) dates.add(d);
            } catch (Exception ignored) {
            }
        }
        dates.sort(Comparator.reverseOrder());
        return dates;
    }

    private boolean hasAnyWeight(JSONArray sets) {
        if (sets == null) return false;
        for (int i = 0; i < sets.length(); i++) {
            JSONObject s = sets.optJSONObject(i);
            if (s != null && !s.optString("weight", "").isEmpty()) return true;
        }
        return false;
    }

    private boolean allSetsAtTop(JSONArray sets, int repMax) {
        if (sets == null || sets.length() == 0) return false;
        boolean sawOne = false;
        for (int i = 0; i < sets.length(); i++) {
            JSONObject s = sets.optJSONObject(i);
            if (s == null) return false;
            String reps = s.optString("reps", "");
            String weight = s.optString("weight", "");
            if (reps.isEmpty() || weight.isEmpty()) return false;
            try {
                if (Integer.parseInt(reps) < repMax) return false;
                sawOne = true;
            } catch (Exception e) {
                return false;
            }
        }
        return sawOne;
    }

    private String summarizeSets(JSONArray sets, String sessionUnit) {
        if (sets == null) return "";
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < sets.length(); i++) {
            JSONObject s = sets.optJSONObject(i);
            if (s == null) continue;
            String w = s.optString("weight", "");
            String r = s.optString("reps", "");
            if (!w.isEmpty() || !r.isEmpty()) {
                parts.add((w.isEmpty() ? "?" : w) + " " + sessionUnit + " × " + (r.isEmpty() ? "?" : r));
            }
        }
        return parts.isEmpty() ? "No completed sets" : String.join("  •  ", parts);
    }

    private Set<LocalDate> savedWeekStarts() {
        Set<LocalDate> weeks = new LinkedHashSet<>();
        for (String key : prefs.getAll().keySet()) {
            try {
                if (key.startsWith(SESSION_PREFIX)) {
                    LocalDate d = LocalDate.parse(key.substring(SESSION_PREFIX.length()));
                    weeks.add(weekStart(d));
                } else if (key.startsWith(LEGACY_WEEK_PREFIX)) {
                    LocalDate d = LocalDate.parse(key.substring(LEGACY_WEEK_PREFIX.length()));
                    weeks.add(weekStart(d));
                }
            } catch (Exception ignored) {
            }
        }
        return weeks;
    }

    private boolean hasImportedLegacyData() {
        if (prefs.getBoolean(LEGACY_IMPORT_COMPLETE, false)) return true;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(LEGACY_WEEK_PREFIX) || key.startsWith(LEGACY_LAST_PREFIX)) return true;
        }
        return false;
    }

    private void showSaved() {
        screen = Screen.SAVED;
        openWeek = null;
        base("Saved Workouts");

        List<LocalDate> weeks = new ArrayList<>(savedWeekStarts());
        weeks.sort(Comparator.reverseOrder());

        int pageCount = Math.max(1, (weeks.size() + WEEKS_PER_PAGE - 1) / WEEKS_PER_PAGE);
        if (savedPage >= pageCount) savedPage = pageCount - 1;
        if (savedPage < 0) savedPage = 0;

        LinearLayout arrows = new LinearLayout(this);
        arrows.setOrientation(LinearLayout.HORIZONTAL);
        arrows.setPadding(0, dp(10), 0, dp(8));

        Button older = compactButton("‹ Older 10");
        Button newer = compactButton("Newer 10 ›");

        boolean hasOlder = (savedPage + 1) * WEEKS_PER_PAGE < weeks.size();
        boolean hasNewer = savedPage > 0;
        setButtonEnabled(older, hasOlder);
        setButtonEnabled(newer, hasNewer);

        older.setOnClickListener(v -> {
            if (hasOlder) {
                savedPage++;
                showSaved();
            }
        });
        newer.setOnClickListener(v -> {
            if (hasNewer) {
                savedPage--;
                showSaved();
            }
        });

        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(50), 1);
        right.leftMargin = dp(10);
        newer.setLayoutParams(right);
        arrows.addView(older);
        arrows.addView(newer);
        content.addView(arrows);

        if (weeks.isEmpty()) {
            LinearLayout c = card();
            c.addView(muted("No saved workouts yet.", 15));
            content.addView(c);
        } else {
            int start = savedPage * WEEKS_PER_PAGE;
            int end = Math.min(start + WEEKS_PER_PAGE, weeks.size());

            for (int i = start; i < end; i++) {
                LocalDate monday = weeks.get(i);
                Button week = button(monday.format(displayDate));
                week.setOnClickListener(v -> showSavedWeek(monday));
                content.addView(week);
            }
        }

        if (!hasImportedLegacyData()) {
            content.addView(spacer(18));
            Button legacy = button("Import Old App Data");
            legacy.setOnClickListener(v -> showLegacyImport());
            content.addView(legacy);
        }

        content.addView(spacer(34));
        Button back = button("Back");
        back.setOnClickListener(v -> showToday());
        content.addView(back);
    }

    private void showSavedWeek(LocalDate monday) {
        screen = Screen.WEEK;
        openWeek = monday;
        base(monday.format(displayDate));

        boolean showedAnything = false;
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            JSONObject session = readSession(date);
            if (session == null) continue;
            showedAnything = true;
            addSavedDayLog(date, session);
        }

        String legacyRaw = prefs.getString(LEGACY_WEEK_PREFIX + monday, "");
        if (!legacyRaw.isEmpty()) {
            showedAnything = true;
            addLegacyLog(monday, legacyRaw);
        }

        if (!showedAnything) {
            LinearLayout c = card();
            c.addView(muted("No saved workouts for this week.", 15));
            content.addView(c);
        }

        content.addView(spacer(34));
        Button back = button("Back");
        back.setOnClickListener(v -> showSaved());
        content.addView(back);
    }

    private void addSavedDayLog(LocalDate date, JSONObject session) {
        LinearLayout c = card();
        TextView h = text(date.format(dayDate), 18, TEXT);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(h);

        String sessionUnit = session.optString("unit", unit);
        JSONArray exercises = session.optJSONArray("exercises");
        if (exercises != null) {
            for (int i = 0; i < exercises.length(); i++) {
                JSONObject e = exercises.optJSONObject(i);
                if (e == null) continue;

                TextView exercise = text(e.optString("name", "Exercise"), 15, TEXT);
                exercise.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                exercise.setPadding(0, dp(10), 0, dp(2));
                c.addView(exercise);

                JSONArray sets = e.optJSONArray("sets");
                boolean any = false;
                if (sets != null) {
                    for (int j = 0; j < sets.length(); j++) {
                        JSONObject s = sets.optJSONObject(j);
                        if (s == null) continue;
                        String w = s.optString("weight", "").trim();
                        String r = s.optString("reps", "").trim();
                        if (w.isEmpty() && r.isEmpty()) continue;
                        any = true;
                        c.addView(muted("Set " + (j + 1) + "    " + (w.isEmpty() ? "?" : w) + " " + sessionUnit + " × " + (r.isEmpty() ? "?" : r) + " reps", 14));
                    }
                }
                if (!any) c.addView(muted("No completed sets", 13));
            }
        }
        content.addView(c);
    }

    private void addLegacyLog(LocalDate monday, String raw) {
        LinearLayout c = card();
        TextView h = text("Imported old-app data", 17, TEXT);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(h);
        c.addView(muted("The previous app did not preserve a separate date for every workout in this week.", 12));

        try {
            JSONObject obj = new JSONObject(raw);
            List<String> names = new ArrayList<>();
            java.util.Iterator<String> it = obj.keys();
            while (it.hasNext()) names.add(it.next());
            names.sort(String::compareToIgnoreCase);
            String legacyUnit = prefs.getString(LEGACY_UNIT, unit);

            for (String name : names) {
                String value = obj.optString(name, "").trim();
                if (value.isEmpty()) continue;
                TextView exercise = text(name, 15, TEXT);
                exercise.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                exercise.setPadding(0, dp(10), 0, dp(1));
                c.addView(exercise);
                String rendered = value.toLowerCase(Locale.US).contains("lb") || value.toLowerCase(Locale.US).contains("kg")
                        ? value
                        : value + " " + legacyUnit;
                c.addView(muted(rendered, 14));
            }
        } catch (Exception ignored) {
            c.addView(muted(raw, 13));
        }

        content.addView(c);
    }

    /**
     * One-time best-effort migration.
     *
     * Android does not allow one installed app to read another app's private SharedPreferences.
     * This importer is useful if legacy preferences are restored into this package by Android,
     * or if this code is ever installed as a signature-compatible update. It also preserves any
     * legacy data already imported by V2.1.
     */
    private void attemptAutomaticLegacyMigration() {
        if (prefs.getBoolean(MIGRATION_CHECKED, false)) return;

        try {
            SharedPreferences possibleLegacy = getSharedPreferences("workout", MODE_PRIVATE);
            Map<String, ?> values = possibleLegacy.getAll();
            if (!values.isEmpty()) importLegacyPreferenceMap(values);
        } catch (Exception ignored) {
        }

        prefs.edit().putBoolean(MIGRATION_CHECKED, true).apply();
    }

    private void importLegacyPreferenceMap(Map<String, ?> values) {
        Object importedUnitValue = values.containsKey("unit") ? values.get("unit") : unit;
        String importedUnit = String.valueOf(importedUnitValue);
        if (!"kg".equals(importedUnit)) importedUnit = "lb";

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LEGACY_UNIT, importedUnit);

        boolean importedAnything = false;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("last_")) {
                String oldName = key.substring(5).replace('_', ' ');
                String id = mapLegacyName(oldName);
                String value = String.valueOf(entry.getValue()).trim();
                if (id != null && !value.isEmpty()) {
                    String cleaned = cleanLegacyWeight(value);
                    if (!cleaned.isEmpty()) {
                        editor.putString(LEGACY_LAST_PREFIX + id, cleaned);
                        importedAnything = true;
                    }
                }
                continue;
            }

            if (!key.startsWith("log_")) continue;

            try {
                LocalDate originalDate = LocalDate.parse(key.substring(4));
                LocalDate monday = weekStart(originalDate);
                JSONObject incoming = new JSONObject(String.valueOf(entry.getValue()));

                JSONObject merged;
                String existing = prefs.getString(LEGACY_WEEK_PREFIX + monday, "");
                if (existing.isEmpty()) merged = new JSONObject();
                else merged = new JSONObject(existing);

                java.util.Iterator<String> names = incoming.keys();
                while (names.hasNext()) {
                    String oldName = names.next();
                    String value = incoming.optString(oldName, "").trim();
                    merged.put(oldName, value);

                    String id = mapLegacyName(oldName);
                    if (id != null && !value.isEmpty()) {
                        String cleaned = cleanLegacyWeight(value);
                        if (!cleaned.isEmpty()) editor.putString(LEGACY_LAST_PREFIX + id, cleaned);
                    }
                }
                editor.putString(LEGACY_WEEK_PREFIX + monday, merged.toString());
                importedAnything = true;
            } catch (Exception ignored) {
            }
        }

        if (importedAnything) editor.putBoolean(LEGACY_IMPORT_COMPLETE, true);
        editor.apply();
    }

    private void showLegacyImport() {
        screen = Screen.IMPORT;
        base("Import Old App Data");

        LinearLayout info = card();
        info.addView(muted("Android keeps the old app's saved workouts in private app storage, so the new app cannot read them directly. Keep the old app installed until this one-time transfer is complete.", 14));
        info.addView(spacer(8));
        info.addView(muted("Run the included export-old-workout-data.bat on a Windows PC with the phone connected by USB and USB debugging enabled. The script exports the old data and copies old_workout.xml into the phone's Downloads folder.", 14));
        content.addView(info);

        Button choose = button("Choose old_workout.xml");
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, PICK_LEGACY_XML);
        });
        content.addView(choose);

        content.addView(spacer(34));
        Button back = button("Back");
        back.setOnClickListener(v -> showSaved());
        content.addView(back);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_LEGACY_XML || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            if (stream == null) throw new Exception("Unable to open file");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) xml.append(line).append('\n');
            reader.close();

            Map<String, Object> values = parseLegacyXml(xml.toString());
            if (values.isEmpty()) {
                Toast.makeText(this, "No old workout data found in that file", Toast.LENGTH_LONG).show();
                return;
            }

            importLegacyPreferenceMap(values);
            prefs.edit().putBoolean(LEGACY_IMPORT_COMPLETE, true).apply();
            Toast.makeText(this, "Old workout data imported", Toast.LENGTH_SHORT).show();
            savedPage = 0;
            showSaved();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to import that file", Toast.LENGTH_LONG).show();
        }
    }

    private Map<String, Object> parseLegacyXml(String xml) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));

        String currentName = null;
        String currentTag = null;
        StringBuilder currentText = null;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("string".equals(tag) || "int".equals(tag) || "long".equals(tag) || "boolean".equals(tag) || "float".equals(tag)) {
                    currentTag = tag;
                    currentName = parser.getAttributeValue(null, "name");
                    currentText = new StringBuilder();
                    if (!"string".equals(tag)) {
                        String value = parser.getAttributeValue(null, "value");
                        if (currentName != null && value != null) values.put(currentName, value);
                    }
                }
            } else if (event == XmlPullParser.TEXT && currentText != null && "string".equals(currentTag)) {
                currentText.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG && currentName != null && parser.getName().equals(currentTag)) {
                if ("string".equals(currentTag)) values.put(currentName, currentText == null ? "" : currentText.toString());
                currentName = null;
                currentTag = null;
                currentText = null;
            }
            event = parser.next();
        }
        return values;
    }

    private String cleanLegacyWeight(String value) {
        String v = value.trim();
        String lower = v.toLowerCase(Locale.US);
        int x = lower.indexOf("×");
        if (x > 0) v = v.substring(0, x).trim();
        v = v.replace("lbs", "").replace("lb", "").replace("kg", "").trim();
        return v;
    }

    private String mapLegacyName(String oldName) {
        String n = oldName.toLowerCase(Locale.US)
                .replace('-', ' ')
                .replace('/', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        if (n.contains("romanian deadlift") || n.equals("rdl")) return "db_rdl";
        if (n.contains("incline") && n.contains("press")) return "incline_db_press";
        if (n.contains("bench") || n.contains("floor press")) return "db_bench";
        if (n.contains("one arm") && n.contains("row")) return "db_row";
        if (n.contains("chest supported") && n.contains("row")) return "chest_supported_db_row";
        if (n.contains("pullover")) return "db_pullover";
        if (n.contains("rear delt")) return "rear_delt_db_fly";
        if (n.contains("lateral raise")) return "db_lateral_raise";
        if (n.contains("overhead press") || n.contains("shoulder press")) return "db_ohp";
        if (n.contains("hammer curl")) return "hammer_curl";
        if (n.contains("triceps") && n.contains("overhead")) return "db_triceps_ext";
        if (n.contains("dumbbell curl") || n.equals("curls") || n.contains("dumbbell curls")) return "db_curl";
        if (n.contains("goblet squat")) return "goblet_squat";
        if (n.contains("split squat")) return "db_split_squat";
        if (n.contains("calf raise")) return "db_calf_raise";
        if (n.contains("weighted crunch") || n.equals("crunch")) return "weighted_crunch";
        if (n.contains("hip thrust") || n.contains("glute bridge")) return "db_hip_thrust";
        return null;
    }

    @Override
    public void onBackPressed() {
        if (screen == Screen.WEEK) {
            showSaved();
            return;
        }
        if (screen == Screen.IMPORT) {
            showSaved();
            return;
        }
        if (screen == Screen.SAVED) {
            showToday();
            return;
        }
        super.onBackPressed();
    }
}
