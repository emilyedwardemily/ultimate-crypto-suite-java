package academy;

import java.io.File;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import crypto.XORUtil;

/**
 * UC-FORTRESS ACADEMY - player progress service.
 *
 * <p>Single source of truth for the new Fortress Academy metrics that do not
 * exist in the legacy Academy code: streaks, practice hours, success rate,
 * daily/weekly/monthly XP, activity feed, generated challenges, simulated
 * global &amp; country rankings and a local AI hint engine.</p>
 *
 * <p>SOLID notes:</p>
 * <ul>
 *   <li>S - owns only academy progress (persistence + metrics + generators).</li>
 *   <li>O - extensible: new cipher families are added via the encoder switch.</li>
 *   <li>L - pure service; the UI layer (Dashboard) depends on this abstraction.</li>
 *   <li>I - narrow, purpose-built getters exposed to the view.</li>
 *   <li>D - persistence behind an internal save/load pair, swappable.</li>
 * </ul>
 *
 * <p>All progress is persisted to {@code ~/.ucsuite/academy_profile.json} so
 * streaks, badges and certificates survive application restarts.</p>
 */
public class AcademyService {

    // ----------------------------------------------------------------
    // CONFIGURATION
    // ----------------------------------------------------------------

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".ucsuite";
    private static final String PROFILE_FILE = APP_DIR + File.separator + "academy_profile.json";
    private static final int MAX_ACTIVITY = 40;

    public static final String[] EASY_FAMILIES =
        {"caesar", "rot13", "reverse", "atbash", "hex", "ascii", "binary", "octal", "base64", "base32", "leet"};
    public static final String[] MEDIUM_FAMILIES =
        {"vigenere", "xor", "morse", "affine", "railfence", "bacon"};
    public static final String[] HARD_FAMILIES =
        {"tripleagent", "xor", "vigenere", "affine"};

    private static final String[] WORDS = {
        "CIPHER", "SECRET", "HACKER", "MOUSE", "CODE", "BYTE", "BITS", "KEY", "LOCK", "HASH",
        "NODE", "DATA", "CLOUD", "ADMIN", "GUEST", "ROOT", "TOKEN", "SALT", "VAULT", "FLAG",
        "HUNT", "ELITE", "PRIME", "NOVA", "ONYX", "GHOST", "CYBER", "FROST", "BLAZE", "SHADOW",
        "CRYPT", "SIGNAL", "RIDDLE", "GAMMA", "BETA", "ALPHA", "DELTA", "ORACLE", "TURING", "ENIGMA",
        "VECTOR", "MATRIX", "KERNEL", "SOCKET", "PROXY", "FIREWALL", "BASTION", "SENTRY", "PALADIN", "ECHO",
        "PHANTOM", "RAVEN", "WOLF", "TIGER", "HAWK", "PANDA", "COBRA", "VIPER", "TITAN", "OMEGA",
        "SIGMA", "DRAGON", "PHOENIX", "MIRAGE", "CRIMSON", "NEON", "VOLT", "STATIC", "RUST", "GLITCH"
    };

    private static final String[] VIGENERE_KEYS = {
        "HACK", "CTF", "XOR", "LOCK", "SAFE", "VIPER", "NODE", "MORSE", "CIPHER", "KEY", "VAULT"
    };

    private static final int[] AFFINE_A = {1, 3, 5, 7, 9, 11, 15, 17, 19, 21, 23, 25};

    private static final String[] BOT_NAMES = {
        "NeonByte", "HashQueen", "ZeroDay", "PhantomX", "CyberMamba", "RavenCode", "KaliGhost",
        "RootKing", "ShellShock", "PacketLoss", "ByteVandal", "CipherNova", "DarkVault", "GridWizard",
        "HexHunter", "IronClad", "JesterBit", "KernelPanic", "LogicBomb", "MatrixMara", "NightOwl",
        "OctalOgre", "ProxyPanda", "QuantumFox", "ReverseRex", "ShadowWolf", "TcpThief", "UnicodeKid",
        "VaultViper", "WifiWizard", "XrayMux", "ZombieNet", "AlphaZero", "BetaBlocker", "CryptoCat",
        "DDoSquatch", "EncryptEagle", "FireStorm", "GhostWriter", "HashBrown", "IntruderIvy", "JavaJack",
        "KernelKoala", "LogicLeap", "MambaMaze", "NexusNine", "OracleOak", "PixelPirate", "QuartzQuill",
        "RansomRhino", "SocketSiren", "TracerTide", "UnixUrsa", "VectorVenom", "WanderWolf", "XenonX",
        "YieldYak", "ZephyrZip"
    };

    private static final String[] COUNTRIES = {
        "Tanzania", "Kenya", "Uganda", "Nigeria", "South Africa", "Egypt", "Ghana", "Morocco",
        "Rwanda", "Ethiopia", "USA", "UK", "Germany", "India", "Brazil", "China", "Japan",
        "France", "Canada", "Australia"
    };

    private static final String[] AVATARS = {
        "\uD83E\uDD8A", "\uD83D\uDC3A", "\uD83E\uDD85", "\uD83E\uDD88", "\uD83D\uDC09",
        "\uD83D\uDC7E", "\uD83E\uDD16", "\uD83D\uDC26", "\uD83D\uDC0D", "\uD83E\uDD81",
        "\uD83D\uDC2F", "\uD83E\uDDE0", "\uD83D\uDC51", "\uD83D\uDEE1\uFE0F", "\u26A1",
        "\uD83D\uDD25", "\uD83D\uDC8E", "\uD83C\uDF0A", "\uD83C\uDF35", "\uD83C\uDF40",
        "\uD83C\uDFAD", "\uD83D\uDD2E"
    };

    private static final String[] FRIEND_NAMES = {
        "KaliQueen", "HexHacker", "BitBender", "RootUser", "CipherCrew", "MorseMan",
        "VaultKeeper", "RSA_Rob", "NeonNadia", "ByteBoss"
    };

    private static final String[] UNIVERSITIES = {
        "Univ of Dar es Salaam", "Strathmore Univ", "Makerere Univ", "Univ of Nairobi",
        "MIT", "Stanford", "Oxford", "ETH Zurich", "IIT Delhi", "U Waterloo",
        "KTH Stockholm", "Univ of Tokyo", "Carnegie Mellon", "Georgia Tech", "Cape Town Univ",
        "Kigali Inst of Tech", "Ashesi Univ", "Addis Ababa Univ", "Univ of Pretoria", "Cairo Univ"
    };

    private static final String[] COMPANIES = {
        "Google", "Microsoft", "Meta", "Amazon", "Apple", "Cisco", "IBM", "Oracle",
        "Vodacom", "Safaricom", "MTN", "Airtel", "Andela", "Paystack", "Flutterwave",
        "NeoBank", "CryptoWare", "Fortress Labs", "HackSec", "Sentinel Ops"
    };

    private static final String[] CRYPTOGRAPHERS = {
        "Alan Turing", "Claude Shannon", "Rivest", "Shamir", "Adleman",
        "Whitfield Diffie", "Martin Hellman", "Bruce Schneier", "Joan Daemen",
        "Vincent Rijmen", "Dan Boneh", "ElGamal", "Koblitz", "Vernam",
        "Kasiski", "Babbage", "Kahn", "Friedman", "Needham", "Kahn-1996"
    };

    // ----------------------------------------------------------------
    // STATE
    // ----------------------------------------------------------------

    private final String operatorId;

    private final Map<String, Integer> solved = new LinkedHashMap<>();
    private int totalXp;
    private int completed;
    private int attempts;
    private int correct;
    private long practiceSeconds;
    private int streak;
    private int bestStreak;
    private String lastSolveDay;

    private final Map<String, Integer> dailyXp = new LinkedHashMap<>();
    private final Map<String, Integer> weeklyXp = new LinkedHashMap<>();
    private final Map<String, Integer> monthlyXp = new LinkedHashMap<>();

    private final ArrayDeque<String> activity = new ArrayDeque<>();

    private final Map<String, Challenge> generated = new LinkedHashMap<>();
    private int genCounter;

    private int coins;
    private int certPoints;
    private int dailyDone;
    private int weeklyDone;
    private int weekendDone;
    private final Set<String> missionDone = new HashSet<>();

    private final Map<String, Integer> moduleUses = new LinkedHashMap<>();
    private int hintUses;
    private int fastSolves;

    private List<Bot> botsCache;

    public AcademyService(String operatorId) {
        this.operatorId = operatorId;
    }

    // ----------------------------------------------------------------
    // PUBLIC API USED BY THE VIEW LAYER
    // ----------------------------------------------------------------

    public String getOperatorId() { return operatorId; }

    public Map<String, Integer> getSolved() { return solved; }
    public int getTotalXp() { return totalXp; }
    public int getSolvedCount() { return completed; }
    public int getRemaining(int total) { return Math.max(0, total - completed); }
    public int getAttempts() { return attempts; }
    public int getCorrect() { return correct; }
    public int getStreak() { return streak; }
    public int getBestStreak() { return bestStreak; }
    public long getPracticeSeconds() { return practiceSeconds; }
    public double getPracticeHours() { return practiceSeconds / 3600.0; }

    /** Success rate as a percentage (0 when no attempts yet). */
    public double getSuccessRate() {
        if (attempts == 0) return 0;
        return (correct * 100.0) / attempts;
    }

    public int getDailyXp() { return dailyXp.getOrDefault(today(), 0); }
    public int getWeeklyXp() { return weeklyXp.getOrDefault(thisWeek(), 0); }
    public int getMonthlyXp() { return monthlyXp.getOrDefault(thisMonth(), 0); }

    // ----- Coins + certificate points -----

    /** In-app currency earned from solving ciphers and completing missions. */
    public int getCoins() { return coins; }

    /** Progress currency that feeds the certificate vault. */
    public int getCertPoints() { return certPoints; }

    public int getDailyDone() { return dailyDone; }
    public int getWeeklyDone() { return weeklyDone; }
    public int getWeekendDone() { return weekendDone; }

    public int getMissionCompletions() { return dailyDone + weeklyDone + weekendDone; }

    // ----- Achievement telemetry -----

    /** How many times the operator opened the given tool module (AES, RSA, XOR, STEGO, HASH...). */
    public int getModuleUses(String module) { return moduleUses.getOrDefault(module, 0); }

    /** Total tool-module usage across all modules. */
    public int getTotalModuleUses() {
        int total = 0;
        for (int v : moduleUses.values()) total += v;
        return total;
    }

    public void recordModuleUse(String module) {
        moduleUses.merge(module, 1, Integer::sum);
        save();
    }

    /** Number of times the AI HINT engine was consulted. */
    public int getHintUses() { return hintUses; }

    public void recordHintUse() {
        hintUses++;
        save();
    }

    /** Number of challenges solved within the 30-second speed-run window. */
    public int getFastSolves() { return fastSolves; }

    public void recordSolveTime(long millis) {
        if (millis >= 0 && millis <= 30_000) fastSolves++;
        save();
    }

    /** Stable key of the currently active mission for the given type. */
    public String getMissionKey(String type) {
        return type + "_" + (type.equals("WEEKLY") || type.equals("WEEKEND") ? thisWeek() : today());
    }

    public boolean isMissionDone(String key) { return missionDone.contains(key); }

    public boolean isWeekendToday() {
        DayOfWeek d = LocalDate.now().getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /**
     * Auto-generates (deterministically per operator + period) the active mission
     * for the given type: DAILY, WEEKLY or WEEKEND.
     */
    public Mission getMission(String type) {
        String period = type.equals("WEEKLY") || type.equals("WEEKEND") ? thisWeek() : today();
        return buildMission(type, period);
    }

    /**
     * Seconds remaining before the mission window closes. DAILY resets at
     * midnight; WEEKLY and WEEKEND roll over on Monday 00:00.
     */
    public long getMissionSecondsLeft(String type) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end;
        if (type.equals("WEEKLY") || type.equals("WEEKEND")) {
            end = now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        } else {
            end = now.toLocalDate().atStartOfDay().plusDays(1);
        }
        return Math.max(0, Duration.between(now, end).getSeconds());
    }

    /**
     * Awards the full mission reward (base XP via {@link #onSolve}, plus bonus
     * XP, coins, certificate points) and marks the mission as completed.
     */
    public void solveMission(Mission m) {
        if (m == null || missionDone.contains(m.key)) return;
        missionDone.add(m.key);
        onSolve(m.challenge.id, m.challenge.xp);
        totalXp += m.bonusXp;
        coins += m.coins;
        certPoints += m.certPoints;
        switch (m.type) {
            case "WEEKLY" -> weeklyDone++;
            case "WEEKEND" -> weekendDone++;
            default -> dailyDone++;
        }
        addActivity("\uD83C\uDFC5 MISSION", m.type + " mission complete! +" + (m.challenge.xp + m.bonusXp)
            + " XP, +" + m.coins + " coins, +" + m.certPoints + " cert pts");
        save();
    }

    public List<String> getActivity() { return new ArrayList<>(activity); }

    public List<Challenge> getGenerated() { return new ArrayList<>(generated.values()); }

    /**
     * Called whenever a challenge is solved (legacy or generated).
     * Keeps the service in sync with the legacy statics and persists.
     */
    public void onSolve(String id, int xp) {
        if (solved.containsKey(id)) return;
        solved.put(id, xp);
        totalXp += xp;
        completed++;
        correct++;
        attempts++;
        updateStreak();
        dailyXp.merge(today(), xp, Integer::sum);
        weeklyXp.merge(thisWeek(), xp, Integer::sum);
        monthlyXp.merge(thisMonth(), xp, Integer::sum);
        coins += 5;
        addActivity("\u2705 SOLVED", "+" + xp + " XP \u2014 " + id);
        save();
    }

    /** Called when an answer is wrong, to feed the success-rate metric. */
    public void recordAttempt(boolean correctGuess) {
        attempts++;
        if (correctGuess) correct++;
        save();
    }

    /** Adds practice seconds (fed by the UI navigation timer). */
    public void addPracticeSeconds(int seconds) {
        practiceSeconds += seconds;
        save();
    }

    /**
     * Generates a brand-new, verifiable challenge for the given difficulty.
     * The generated challenge is persisted so it survives restarts.
     */
    public Challenge generateChallenge(String difficulty) {
        String fam;
        switch (difficulty == null ? "EASY" : difficulty) {
            case "MEDIUM" -> fam = MEDIUM_FAMILIES[rnd().nextInt(MEDIUM_FAMILIES.length)];
            case "HARD"   -> fam = HARD_FAMILIES[rnd().nextInt(HARD_FAMILIES.length)];
            default       -> fam = EASY_FAMILIES[rnd().nextInt(EASY_FAMILIES.length)];
        }
        Challenge ch = buildChallenge(fam, difficulty);
        generated.put(ch.id, ch);
        save();
        return ch;
    }

    public void removeGenerated(String id) {
        generated.remove(id);
        save();
    }

    /** Deterministic simulated global + country position among a network of operators. */
    public GlobalPosition getGlobalPosition() {
        List<Bot> all = new ArrayList<>(bots());
        all.add(new Bot(operatorId, myCountry(), totalXp));
        all.sort((a, b) -> {
            if (b.xp != a.xp) return Integer.compare(b.xp, a.xp);
            return a.name.compareTo(b.name);
        });
        String country = myCountry();

        int globalRank = 1;
        for (Bot b : all) {
            if (b.name.equals(operatorId)) break;
            globalRank++;
        }
        int countryCount = 0;
        int countryRank = 1;
        for (Bot b : all) {
            if (b.country.equals(country)) {
                countryCount++;
                if (b.name.equals(operatorId)) { break; }
                countryRank++;
            }
        }
        return new GlobalPosition(globalRank, all.size(), countryRank, countryCount, country);
    }

    /** Progressive AI hint engine. Level 1 = gentle, 2 = tactical, 3 = reveal. */
    public String aiHint(Challenge ch, int level) {
        String fam = ch.family == null ? "" : ch.family;
        switch (level) {
            case 2 -> {
                return tacticalHint(fam) + (ch.hint == null || ch.hint.isEmpty() ? "" : "  (" + ch.hint + ")");
            }
            case 3 -> {
                return "\uD83D\uDD11 DISCLOSURE CLEARANCE GRANTED \u2014 the flag is: " + ch.flag;
            }
            default -> {
                return "This challenge runs on the \u201C" + familyName(fam)
                    + "\u201D engine. Open SIMULATE, type a sample word, and watch the cipher step by step before attacking the ciphertext.";
            }
        }
    }

    /** Deterministic mentor tip that reacts to the operator's progress. */
    public String mentorTip() {
        if (completed == 0) {
            return "Welcome to the Fortress, operator. Start with the EASY tier \u2014 Caesar, ROT13 and Hex are the perfect first ciphers to master.";
        }
        if (completed < 5) {
            return "Solid start. Chain your EASY wins \u2014 every milestone unlocks a new badge in your profile.";
        }
        if (completed < 10) {
            return "You are building momentum. Keep your streak alive: solve at least one cipher every day.";
        }
        if (completed < 20) {
            return "Time to graduate to MEDIUM: Vigenere, XOR and Morse teach you how keyed ciphers really work.";
        }
        if (completed < 50) {
            return "Excellent discipline. Attack Affine and Rail Fence \u2014 the ciphertext always hides a repeating pattern.";
        }
        if (completed < 100) {
            return "HARD tier awaits: Triple-Stack layers three ciphers. Peel the stack top-down, one layer at a time.";
        }
        if (completed < 200) {
            return "Grandmaster territory. The final stretch is brutal \u2014 keep the completionist badge in sight.";
        }
        return "Fortress Legend. You have cleared nearly every cipher in the Academy \u2014 the network knows your name, operator.";
    }

    public void resetAll() {
        solved.clear();
        totalXp = 0;
        completed = 0;
        attempts = 0;
        correct = 0;
        practiceSeconds = 0;
        streak = 0;
        bestStreak = 0;
        lastSolveDay = null;
        dailyXp.clear();
        weeklyXp.clear();
        monthlyXp.clear();
        activity.clear();
        generated.clear();
        genCounter = 0;
        coins = 0;
        certPoints = 0;
        dailyDone = 0;
        weeklyDone = 0;
        weekendDone = 0;
        missionDone.clear();
        moduleUses.clear();
        hintUses = 0;
        fastSolves = 0;
        save();
    }

    // ----------------------------------------------------------------
    // PERSISTENCE
    // ----------------------------------------------------------------

    public void load() {
        try {
            File f = new File(PROFILE_FILE);
            if (!f.exists()) return;
            String raw = Files.readString(f.toPath());
            JSONObject j = new JSONObject(raw);
            if (!operatorId.equals(j.optString("operatorId", operatorId))) {
                return;
            }
            JSONObject sol = j.optJSONObject("solved");
            if (sol != null) for (String k : sol.keySet()) solved.put(k, sol.optInt(k));
            totalXp = j.optInt("totalXp");
            completed = j.optInt("completed");
            attempts = j.optInt("attempts");
            correct = j.optInt("correct");
            practiceSeconds = j.optLong("practiceSeconds");
            streak = j.optInt("streak");
            bestStreak = j.optInt("bestStreak");
            lastSolveDay = j.optString("lastSolveDay", null);
            genCounter = j.optInt("genCounter");
            coins = j.optInt("coins");
            certPoints = j.optInt("certPoints");
            dailyDone = j.optInt("dailyDone");
            weeklyDone = j.optInt("weeklyDone");
            weekendDone = j.optInt("weekendDone");
            JSONArray md = j.optJSONArray("missionsDone");
            if (md != null) for (int i = 0; i < md.length(); i++) missionDone.add(md.getString(i));
            readBucket(j, "moduleUses", moduleUses);
            hintUses = j.optInt("hintUses");
            fastSolves = j.optInt("fastSolves");
            readBucket(j, "dailyXp", dailyXp);
            readBucket(j, "weeklyXp", weeklyXp);
            readBucket(j, "monthlyXp", monthlyXp);
            JSONArray act = j.optJSONArray("activity");
            if (act != null) for (int i = 0; i < act.length(); i++) activity.addLast(act.getString(i));
            JSONArray gen = j.optJSONArray("generated");
            if (gen != null) {
                for (int i = 0; i < gen.length(); i++) {
                    JSONObject c = gen.getJSONObject(i);
                    Challenge ch = new Challenge(
                        c.getString("id"), c.getString("title"), c.optInt("stars"),
                        c.optInt("xp"), c.optString("diff"), c.optString("family"),
                        c.optString("descr"), c.optString("hint"), c.getString("flag"));
                    generated.put(ch.id, ch);
                }
            }
        } catch (Exception e) {
            System.err.println("[ACADEMY] Profile load failed (safe ignore): " + e.getMessage());
        }
    }

    public void save() {
        try {
            File dir = new File(APP_DIR);
            if (!dir.exists()) dir.mkdirs();
            JSONObject j = new JSONObject();
            j.put("operatorId", operatorId);
            JSONObject sol = new JSONObject();
            for (Map.Entry<String, Integer> e : solved.entrySet()) sol.put(e.getKey(), e.getValue());
            j.put("solved", sol);
            j.put("totalXp", totalXp);
            j.put("completed", completed);
            j.put("attempts", attempts);
            j.put("correct", correct);
            j.put("practiceSeconds", practiceSeconds);
            j.put("streak", streak);
            j.put("bestStreak", bestStreak);
            j.put("lastSolveDay", lastSolveDay == null ? "" : lastSolveDay);
            j.put("genCounter", genCounter);
            j.put("coins", coins);
            j.put("certPoints", certPoints);
            j.put("dailyDone", dailyDone);
            j.put("weeklyDone", weeklyDone);
            j.put("weekendDone", weekendDone);
            JSONArray md = new JSONArray();
            for (String k : missionDone) md.put(k);
            j.put("missionsDone", md);
            writeBucket(j, "moduleUses", moduleUses);
            j.put("hintUses", hintUses);
            j.put("fastSolves", fastSolves);
            writeBucket(j, "dailyXp", dailyXp);
            writeBucket(j, "weeklyXp", weeklyXp);
            writeBucket(j, "monthlyXp", monthlyXp);
            JSONArray act = new JSONArray();
            for (String a : activity) act.put(a);
            j.put("activity", act);
            JSONArray gen = new JSONArray();
            for (Challenge c : generated.values()) {
                JSONObject co = new JSONObject();
                co.put("id", c.id); co.put("title", c.title); co.put("stars", c.stars);
                co.put("xp", c.xp); co.put("diff", c.diff); co.put("family", c.family);
                co.put("descr", c.descr); co.put("hint", c.hint); co.put("flag", c.flag);
                gen.put(co);
            }
            j.put("generated", gen);
            Files.writeString(new File(PROFILE_FILE).toPath(), j.toString(2));
        } catch (Exception e) {
            System.err.println("[ACADEMY] Profile save failed (safe ignore): " + e.getMessage());
        }
    }

    private static void readBucket(JSONObject j, String key, Map<String, Integer> out) {
        JSONObject o = j.optJSONObject(key);
        if (o == null) return;
        for (String k : o.keySet()) out.put(k, o.optInt(k));
    }

    private static void writeBucket(JSONObject j, String key, Map<String, Integer> in) {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Integer> e : in.entrySet()) o.put(e.getKey(), e.getValue());
        j.put(key, o);
    }

    // ----------------------------------------------------------------
    // STREAK + ACTIVITY
    // ----------------------------------------------------------------

    private void updateStreak() {
        String today = today();
        if (today.equals(lastSolveDay)) return;
        if (lastSolveDay != null && yesterday().equals(lastSolveDay)) {
            streak++;
        } else {
            streak = 1;
        }
        lastSolveDay = today;
        if (streak > bestStreak) bestStreak = streak;
    }

    private void addActivity(String action, String detail) {
        String line = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d HH:mm")) + " | " + action + " | " + detail;
        activity.addFirst(line);
        while (activity.size() > MAX_ACTIVITY) activity.removeLast();
    }

    // ----------------------------------------------------------------
    // CHALLENGE GENERATOR + ENCODERS
    // ----------------------------------------------------------------

    private Challenge buildChallenge(String fam, String difficulty) {
        return buildChallenge(fam, difficulty, rnd());
    }

    private Challenge buildChallenge(String fam, String difficulty, Random r) {
        String word = WORDS[r.nextInt(WORDS.length)];
        String flag = "UC{" + word + "}";
        String diff = difficulty == null ? defaultDiff(fam) : difficulty;
        String id = "gen_" + (++genCounter);
        String title = titleFor(fam);
        String descr;
        String hint;
        int stars;
        int xp;

        switch (fam) {
            case "caesar" -> {
                int s = 1 + rnd().nextInt(25);
                stars = 1; xp = 100;
                descr = "Caesar +" + s + ". Decrypt: " + caesar(word, s);
                hint = "Shift each letter back by " + s + " positions.";
            }
            case "rot13" -> {
                stars = 1; xp = 80;
                descr = "ROT13: " + caesar(word, 13);
                hint = "ROT13 shifts exactly 13.";
            }
            case "reverse" -> {
                stars = 1; xp = 60;
                descr = "Reverse: " + reverse(word);
                hint = "Read the string backwards.";
            }
            case "atbash" -> {
                stars = 1; xp = 90;
                descr = "Atbash -> decrypt: " + atbash(word);
                hint = "A\u2194Z, B\u2194Y, C\u2194X \u2026 (mirror the alphabet).";
            }
            case "hex" -> {
                stars = 1; xp = 90;
                descr = "Hex -> ASCII: " + toHexStr(flag.getBytes());
                hint = "Every 2 hex chars = 1 byte.";
            }
            case "ascii" -> {
                stars = 1; xp = 80;
                descr = "ASCII codes: [" + asciiCodes(word) + "]";
                hint = "Each number 0-127 = one ASCII char.";
            }
            case "binary" -> {
                stars = 1; xp = 110;
                descr = "Binary -> ASCII: " + toBinaryStr(flag.getBytes());
                hint = "Every 8 bits = one byte.";
            }
            case "octal" -> {
                stars = 1; xp = 100;
                descr = "Octal -> ASCII: " + octalCodes(word);
                hint = "Each 3-digit octal number = one byte.";
            }
            case "base64" -> {
                stars = 1; xp = 120;
                descr = "Base64 decode: " + java.util.Base64.getEncoder().encodeToString(word.getBytes());
                hint = "Base64 packs 3 bytes into 4 chars.";
            }
            case "base32" -> {
                stars = 1; xp = 120;
                descr = "Base32 decode: " + base32(word);
                hint = "Base32 uses A-Z and 2-7. Uppercase output.";
            }
            case "leet" -> {
                stars = 2; xp = 160;
                descr = "1337: " + leet(word);
                hint = "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.";
            }
            case "morse" -> {
                stars = 2; xp = 170;
                descr = "Morse: " + morse(word);
                hint = "Dots and dashes: . = dot, - = dash. Spaces separate letters.";
            }
            case "bacon" -> {
                stars = 3; xp = 230;
                descr = "Baconian: " + bacon(word);
                hint = "Baconian: each letter = 5 a/b symbols (a=0,b=1).";
            }
            case "affine" -> {
                int a = AFFINE_A[r.nextInt(AFFINE_A.length)];
                int b = r.nextInt(26);
                stars = 3; xp = 260;
                descr = "Affine -> decrypt: " + affine(word, a, b);
                hint = "Affine: c=(a*p+b) mod 26, a=" + a + ", b=" + b + ".";
            }
            case "railfence" -> {
                int rails = 2 + r.nextInt(4);
                stars = 3; xp = 240;
                descr = "Rail fence (" + rails + " rails): " + railFence(word, rails);
                hint = "Rail fence with " + rails + " rails. Read in zig-zag.";
            }
            case "vigenere" -> {
                String key = VIGENERE_KEYS[r.nextInt(VIGENERE_KEYS.length)];
                stars = 2; xp = 200;
                descr = "Vigenere: " + vigenere(word, key);
                hint = "Key=" + key + ". Each letter shifted by key index.";
            }
            case "xor" -> {
                String key = diff.equals("HARD")
                    ? String.format("%c%c%c", keyChar(r), keyChar(r), keyChar(r))
                    : String.valueOf(keyChar(r));
                stars = diff.equals("HARD") ? 4 : 2;
                xp = diff.equals("HARD") ? 380 : 180;
                descr = "XOR (key " + key + "): " + XORUtil.encrypt(word, key);
                hint = "XOR key='" + key + "'. Use XOR tool -> Decrypt.";
            }
            case "tripleagent" -> {
                String key = VIGENERE_KEYS[r.nextInt(VIGENERE_KEYS.length)];
                stars = 5; xp = 450;
                descr = "Triple-Stack: " + tripleAgent(word, key);
                hint = "Decrypt: ROT13 -> Reverse -> Vigenere, key=" + key + ".";
            }
            default -> {
                stars = 1; xp = 100;
                descr = fam + ": " + word;
                hint = "Analyze the cipher and reverse it.";
            }
        }

        return new Challenge(id, title, stars, xp, diff, fam, descr, hint, flag);
    }

    /** Builds the auto-generated mission for a period, seeded deterministically per operator. */
    private Mission buildMission(String type, String period) {
        Random r = new Random((operatorId + "|" + type + "|" + period).hashCode());
        String key = type + "_" + period;
        String fam;
        String diff;
        int bonusXp;
        int coins;
        int certPoints;
        String badge;
        switch (type) {
            case "WEEKLY" -> {
                diff = "MEDIUM";
                fam = MEDIUM_FAMILIES[r.nextInt(MEDIUM_FAMILIES.length)];
                bonusXp = 400; coins = 100; certPoints = 20; badge = "weekly_raider";
            }
            case "WEEKEND" -> {
                diff = "HARD";
                fam = HARD_FAMILIES[r.nextInt(HARD_FAMILIES.length)];
                bonusXp = 800; coins = 250; certPoints = 50; badge = "weekend_warrior";
            }
            default -> {
                diff = "EASY";
                fam = EASY_FAMILIES[r.nextInt(EASY_FAMILIES.length)];
                bonusXp = 150; coins = 25; certPoints = 5; badge = "daily_grinder";
            }
        }
        Challenge ch = buildChallenge(fam, diff, r);
        ch = new Challenge(key, ch.title, ch.stars, ch.xp, ch.diff, ch.family, ch.descr, ch.hint, ch.flag);
        boolean done = missionDone.contains(key);
        return new Mission(key, type, type.equals("WEEKLY") || type.equals("WEEKEND")
            ? thisWeek() : today(), ch, bonusXp, coins, certPoints, badge, done);
    }

    private char keyChar(Random r) {
        return (char) ('A' + r.nextInt(26));
    }

    private static String defaultDiff(String fam) {
        for (String f : HARD_FAMILIES) if (f.equals(fam)) return "HARD";
        for (String f : MEDIUM_FAMILIES) if (f.equals(fam)) return "MEDIUM";
        return "EASY";
    }

    private static String titleFor(String fam) {
        return switch (fam) {
            case "caesar" -> "Caesar Shift";
            case "rot13" -> "ROT13 Flip";
            case "reverse" -> "Backwards Brain";
            case "atbash" -> "Atbash Mirror";
            case "hex" -> "Hex Runner";
            case "ascii" -> "ASCII Lab";
            case "binary" -> "Binary Decoder";
            case "octal" -> "Octal Lab";
            case "base64" -> "Base64 Breaker";
            case "base32" -> "Base32 Coder";
            case "leet" -> "1337 Speak";
            case "morse" -> "Morse Mania";
            case "bacon" -> "Bacon's Bite";
            case "affine" -> "Affine Formula";
            case "railfence" -> "Rail Fence";
            case "vigenere" -> "Vigenere Gate";
            case "xor" -> "XOR Breaker";
            case "tripleagent" -> "Triple Agent";
            default -> "Practice Cipher";
        };
    }

    public static String familyName(String family) {
        if (family == null) return "Cipher";
        return switch (family) {
            case "caesar" -> "Caesar Cipher";
            case "rot13" -> "ROT13";
            case "reverse" -> "String Reversal";
            case "atbash" -> "Atbash";
            case "hex" -> "Hex Encoding";
            case "ascii" -> "ASCII Encoding";
            case "binary" -> "Binary Encoding";
            case "octal" -> "Octal Encoding";
            case "base64" -> "Base64";
            case "base32" -> "Base32";
            case "leet" -> "1337 Speak";
            case "morse" -> "Morse Code";
            case "bacon" -> "Baconian Cipher";
            case "affine" -> "Affine Cipher";
            case "railfence" -> "Rail Fence";
            case "vigenere" -> "Vigenere Cipher";
            case "xor" -> "XOR Bitwise";
            case "tripleagent" -> "Triple-Stack Cipher";
            case "caesarhard" -> "Caesar Fortress";
            case "vigenerehard" -> "Vigenere Fortress";
            case "morsehard" -> "Morse Fortress";
            case "xorhard" -> "XOR Fortress";
            case "railhard" -> "Rail Fortress";
            case "affinehard" -> "Affine Fortress";
            default -> family.toUpperCase();
        };
    }

    private static String tacticalHint(String fam) {
        return switch (fam) {
            case "caesar", "caesarhard" -> "There are only 25 possible shifts. Brute-force them one at a time.";
            case "rot13" -> "ROT13 is symmetric \u2014 encode and decode are the same operation.";
            case "reverse" -> "Flip the whole string end-to-end, character by character.";
            case "atbash" -> "Mirror the alphabet: A becomes Z, B becomes Y, and so on.";
            case "hex" -> "Group the digits in pairs and convert each pair to a character (e.g. 43='C', 49='I').";
            case "ascii" -> "Split the numbers and convert each one using the ASCII table.";
            case "binary" -> "Chop the bit string into 8-bit groups, then convert each byte.";
            case "octal" -> "Each 3-digit octal group maps to one byte \u2014 convert them to decimal then ASCII.";
            case "base64" -> "Base64 output uses A-Z, a-z, 0-9, +, / and trailing = padding.";
            case "base32" -> "Base32 output uses only A-Z and 2-7, and may end in = padding.";
            case "leet" -> "Replace digits/symbols back to letters: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.";
            case "morse" -> "Dots and dashes are letters; slashes or spaces are word separators.";
            case "bacon" -> "Every 5 a/b symbols encode one letter (a=0, b=1) \u2014 binary to index 0-25.";
            case "affine", "affinehard" -> "Work backward: p = a^(-1) * (c - b) mod 26. Invert a first.";
            case "railfence", "railhard" -> "Write the ciphertext down a zig-zag fence of N rails, then read each rail left-to-right.";
            case "vigenere", "vigenerehard" -> "Subtract the key letter value from each ciphertext letter, mod 26.";
            case "xor", "xorhard" -> "XOR is its own inverse: xor the bytes with the key again to recover the flag.";
            case "tripleagent" -> "Reverse the stack in order: ROT13, then Reverse, then Vigenere with the key.";
            default -> "Identify the cipher family, then reverse the exact transformation described in the hint.";
        };
    }

    // ----- Encoder primitives (mirror the playground simulations) -----

    private static String caesar(String s, int shift) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append((char) (((c - 'A' + shift) % 26) + 'A'));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    private static String atbash(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append((char) ('A' + (25 - (c - 'A'))));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String asciiCodes(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(sb.length() == 0 ? "" : " ").append((int) c);
        return sb.toString();
    }

    private static String octalCodes(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(sb.length() == 0 ? "" : " ").append(Integer.toOctalString(c));
        return sb.toString();
    }

    private static String toHexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private static String toBinaryStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        return sb.toString();
    }

    private static String base32(String s) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        byte[] bytes = s.getBytes();
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                out.append(alphabet.charAt((buffer >> bits) & 0x1F));
            }
        }
        if (bits > 0) out.append(alphabet.charAt((buffer << (5 - bits)) & 0x1F));
        while (out.length() % 8 != 0) out.append('=');
        return out.toString();
    }

    private static String leet(String s) {
        Map<Character, Character> map = Map.of(
            'A', '4', 'E', '3', 'I', '1', 'O', '0', 'S', '5', 'T', '7', 'G', '9', 'B', '8');
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) sb.append(map.getOrDefault(c, c));
        return sb.toString();
    }

    private static String morse(String s) {
        String[] mc = {".-","-...","-.-.","-..",".","..-.","--.","....","..",".---","-.-",".-..",
            "--","-.","---",".--.","--.-",".-.","...","-","..-","...-",".--","-..-","-.--","--.."};
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                if (sb.length() > 0) sb.append(' ');
                sb.append(mc[c - 'A']);
            }
        }
        return sb.toString();
    }

    private static String bacon(String s) {
        String[] bacon = {"aaaaa","aaaab","aaaba","aaabb","aabaa","aabab","aabba","aabbb","abaaa","abaab",
            "ababa","ababb","abbaa","abbab","abbba","abbbb","baaaa","baaab","baaba","baabb","babaa","babab","babba","babbb","bbaaa","bbaab"};
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append(sb.length() == 0 ? "" : " ").append(bacon[c - 'A']);
        }
        return sb.toString();
    }

    private static String affine(String s, int a, int b) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') sb.append((char) (((a * (c - 'A') + b) % 26) + 'A'));
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String railFence(String s, int rails) {
        int n = s.length();
        char[][] fence = new char[rails][n];
        for (char[] row : fence) java.util.Arrays.fill(row, ' ');
        int r = 0, d = 1;
        for (int i = 0; i < n; i++) {
            fence[r][i] = s.charAt(i);
            r += d;
            if (r == rails - 1) d = -1;
            else if (r == 0) d = 1;
        }
        StringBuilder sb = new StringBuilder();
        for (char[] row : fence) for (char c : row) if (c != ' ') sb.append(c);
        return sb.toString();
    }

    private static String vigenere(String s, String key) {
        key = key.toUpperCase();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : s.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                char k = key.charAt(i % key.length());
                sb.append((char) (((c - 'A' + (k - 'A')) % 26) + 'A'));
                i++;
            } else sb.append(c);
        }
        return sb.toString();
    }

    private static String tripleAgent(String s, String key) {
        return caesar(reverse(vigenere(s, key)), 13);
    }

    // ----------------------------------------------------------------
    // SIMULATED GLOBAL NETWORK
    // ----------------------------------------------------------------

    private static final class Bot {
        final String name, country, avatar;
        final int xp;
        Bot(String name, String country, int xp) { this(name, country, xp, null); }
        Bot(String name, String country, int xp, String avatar) {
            this.name = name; this.country = country; this.xp = xp; this.avatar = avatar;
        }
    }

    /** Deterministic, stable pool of operators used for simulated global rankings. */
    private List<Bot> bots() {
        if (botsCache == null) {
            List<Bot> list = new ArrayList<>();
            Random r = new Random(operatorId.hashCode());
            for (String name : BOT_NAMES) {
                String country = COUNTRIES[r.nextInt(COUNTRIES.length)];
                int xp = 250 + r.nextInt(14000);
                list.add(new Bot(name, country, xp));
            }
            botsCache = list;
        }
        return botsCache;
    }

    private String myCountry() {
        return COUNTRIES[Math.abs(operatorId.hashCode()) % COUNTRIES.length];
    }

    private Random rnd() { return new Random(); }

    // ----------------------------------------------------------------
    // DATE HELPERS
    // ----------------------------------------------------------------

    private static String today() { return LocalDate.now().toString(); }
    private static String yesterday() { return LocalDate.now().minusDays(1).toString(); }
    private static String thisWeek() {
        LocalDate d = LocalDate.now();
        int w = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return d.getYear() + "-W" + String.format("%02d", w);
    }
    private static String thisMonth() { return LocalDate.now().toString().substring(0, 7); }

    /** An auto-generated, time-boxed mission with its full reward bundle. */
    public static final class Mission {
        public final String key;
        public final String type;
        public final String period;
        public final Challenge challenge;
        public final int bonusXp;
        public final int coins;
        public final int certPoints;
        public final String badge;
        public final boolean done;
        public Mission(String key, String type, String period, Challenge challenge,
                       int bonusXp, int coins, int certPoints, String badge, boolean done) {
            this.key = key; this.type = type; this.period = period;
            this.challenge = challenge; this.bonusXp = bonusXp; this.coins = coins;
            this.certPoints = certPoints; this.badge = badge; this.done = done;
        }
    }

    /** Immutable result describing the operator position inside the simulated network. */
    public static final class GlobalPosition {
        public final int globalRank, globalCount, countryRank, countryCount;
        public final String country;
        public GlobalPosition(int globalRank, int globalCount, int countryRank, int countryCount, String country) {
            this.globalRank = globalRank;
            this.globalCount = globalCount;
            this.countryRank = countryRank;
            this.countryCount = countryCount;
            this.country = country;
        }
    }

    /** A single row inside the simulated network leaderboards. */
    public static final class Standing {
        public final int rank;
        public final String name;
        public final String country;
        public final int xp;
        public final int level;
        public final String avatar;
        public final int badges;
        public final boolean me;
        public Standing(int rank, String name, String country, int xp, int level, String avatar, int badges, boolean me) {
            this.rank = rank; this.name = name; this.country = country; this.xp = xp;
            this.level = level; this.avatar = avatar; this.badges = badges; this.me = me;
        }
    }

    /** Display level derived from XP, mirroring the legacy {@code totalXP / 200 + 1} convention. */
    public static int levelFor(int xp) { return xp / 200 + 1; }

    /** Deterministic badge count derived from XP. */
    public static int badgesFor(int xp) { return Math.min(xp / 400 + 1, 12); }

    /** Deterministic avatar glyph for any operator name. */
    public static String avatarFor(String name) {
        return AVATARS[(name.hashCode() & 0x7fffffff) % AVATARS.length];
    }

    /** Sorts a pool by XP (ties by name) and returns the top-N as {@link Standing} rows. */
    private List<Standing> ranked(int topN, List<Bot> all) {
        all.sort((a, b) -> {
            if (b.xp != a.xp) return Integer.compare(b.xp, a.xp);
            return a.name.compareTo(b.name);
        });
        List<Standing> out = new ArrayList<>();
        int rank = 1;
        for (Bot b : all) {
            out.add(new Standing(rank, b.name, b.country, b.xp, levelFor(b.xp),
                b.avatar == null ? avatarFor(b.name) : b.avatar, badgesFor(b.xp), b.name.equals(operatorId)));
            rank++;
            if (out.size() >= topN) break;
        }
        return out;
    }

    /** Top-N standings for the global leaderboard view. */
    public List<Standing> getGlobalStandings(int topN) {
        List<Bot> all = new ArrayList<>(bots());
        all.add(new Bot(operatorId, myCountry(), totalXp));
        return ranked(topN, all);
    }

    /** Top-N standings for the weekly leaderboard, using this week's real earned XP. */
    public List<Standing> getWeeklyStandings(int topN) {
        String week = thisWeek();
        Random r = new Random((operatorId + "|" + week).hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : BOT_NAMES) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 20 + r.nextInt(900)));
        }
        all.add(new Bot(operatorId, myCountry(), weeklyXp.getOrDefault(week, 0)));
        return ranked(topN, all);
    }

    /** Top-N standings for the monthly leaderboard, using this month's real earned XP. */
    public List<Standing> getMonthlyStandings(int topN) {
        String month = thisMonth();
        Random r = new Random((operatorId + "|" + month).hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : BOT_NAMES) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 80 + r.nextInt(4200)));
        }
        all.add(new Bot(operatorId, myCountry(), monthlyXp.getOrDefault(month, 0)));
        return ranked(topN, all);
    }

    /** Top-N standings for the friends leaderboard. */
    public List<Standing> getFriendsStandings(int topN) {
        Random r = new Random((operatorId + "|friends").hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : FRIEND_NAMES) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 300 + r.nextInt(6000)));
        }
        all.add(new Bot(operatorId, myCountry(), totalXp));
        return ranked(topN, all);
    }

    /** Top-N simulated universities ranked by aggregate team XP. */
    public List<Standing> getTopUniversities(int topN) {
        Random r = new Random((operatorId + "|universities").hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : UNIVERSITIES) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 20000 + r.nextInt(120000), "\uD83C\uDF93"));
        }
        return ranked(topN, all);
    }

    /** Top-N simulated companies ranked by aggregate team XP. */
    public List<Standing> getTopCompanies(int topN) {
        Random r = new Random((operatorId + "|companies").hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : COMPANIES) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 15000 + r.nextInt(90000), "\uD83C\uDFE2"));
        }
        return ranked(topN, all);
    }

    /** Top-N legendary cryptographers ranked by lifetime reputation. */
    public List<Standing> getTopCryptographers(int topN) {
        Random r = new Random((operatorId + "|cryptographers").hashCode());
        List<Bot> all = new ArrayList<>();
        for (String name : CRYPTOGRAPHERS) {
            all.add(new Bot(name, COUNTRIES[r.nextInt(COUNTRIES.length)], 90000 + r.nextInt(1100000), "\uD83E\uDDE0"));
        }
        return ranked(topN, all);
    }
}
