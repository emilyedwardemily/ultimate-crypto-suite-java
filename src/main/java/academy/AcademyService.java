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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

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
        {"vigenere", "xor", "morse", "affine", "railfence", "bacon", "playfair", "transposition"};
    public static final String[] HARD_FAMILIES =
        {"tripleagent", "xor", "vigenere", "affine", "aes", "hill"};
    public static final String[] EXPERT_FAMILIES =
        {"affine", "vigenere", "xor", "rsa", "hashing"};
    public static final String[] NIGHTMARE_FAMILIES =
        {"tripleagent", "xor", "stego", "forensics"};
    public static final String[] IMPOSSIBLE_FAMILIES =
        {"tripleagent", "xor", "stego"};

    /** Families the AI Challenge Generator can summon on demand (item 14). */
    public static final String[] GENERATOR_FAMILIES = {
        "random", "caesar", "rot13", "reverse", "atbash", "hex", "ascii", "binary",
        "base64", "base32", "leet", "vigenere", "xor", "morse", "affine", "railfence",
        "bacon", "aes", "rsa", "stego", "hashing", "forensics", "playfair", "hill", "transposition"
    };

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

    /** The 16 career ranks: {minXp, name, icon}. Script Kiddie is rank 0. */
    public static final String[][] CAREER_RANKS = {
        {"0",     "Script Kiddie",     "\uD83D\uDCBB"},
        {"100",   "Recruit",           "\uD83E\uDD4A"},
        {"250",   "Junior Analyst",    "\uD83D\uDD0D"},
        {"500",   "SOC Analyst",       "\uD83D\uDEE1\uFE0F"},
        {"900",   "Security Engineer", "\uD83D\uDD27"},
        {"1400",  "Incident Responder","\uD83D\uDE91"},
        {"2000",  "Pen Tester",        "\uD83D\uDD2E"},
        {"2700",  "Red Team",          "\uD83D\uDD25"},
        {"3500",  "Blue Team",         "\uD83D\uDEE1\uFE0F"},
        {"4500",  "Purple Team",       "\uD83E\uDD84"},
        {"5700",  "Threat Hunter",     "\uD83D\uDC3E"},
        {"7000",  "Malware Analyst",   "\uD83E\uDDE0"},
        {"8500",  "Security Architect","\uD83C\uDFDB\uFE0F"},
        {"10000", "Cryptographer",     "\uD83D\uDD10"},
        {"12500", "Cyber Commander",   "\uD83E\uDDD1\u200D\uD83D\uDC68\u200D\uD83D\uDCBC"},
        {"15000", "Cyber Legend",      "\uD83C\uDFC6"}
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

    private final Map<String, Integer> weakFamilies = new LinkedHashMap<>();

    private final Map<String, Long> fastestMs = new LinkedHashMap<>();
    private final Map<String, Long> totalMs = new LinkedHashMap<>();
    private final Map<String, Integer> solveTimesCount = new LinkedHashMap<>();

    // ----- Career mode (item 16) -----
    private int careerRank;
    private final List<String> rankHistory = new ArrayList<>();

    // ----- Competitive honours (items 17/18) -----
    private int pvpWins;
    private int tournamentWins;

    // ----- Profile customization (item 20) -----
    private String avatar = "\uD83D\uDD75\uFE0F";
    private String bio = "Cryptographic operator in training.";
    private String country = COUNTRIES[0];
    private String university = "";

    private List<Bot> botsCache;

    // ----- Notifications (item 21) -----
    private final ArrayDeque<Notif> notifications = new ArrayDeque<>();
    private static final int MAX_NOTIFS = 50;

    // ----- Settings (item 23) -----
    private final Map<String, String> settings = new LinkedHashMap<>();

    // ----- Security (item 24) -----
    private static final String SIG_SALT = "UC-ACADEMY-INTEGRITY-v1";
    private String storedSig = "";
    private boolean sigOk = true;
    private long lastScan = 0;
    private int scanPassed = -1;

    // ----- Performance cache (item 25) -----
    private static final long CACHE_TTL_MS = 30_000;
    private final Map<String, CacheEntry> standingsCache = new LinkedHashMap<>();
    private long cacheHits;
    private long cacheMisses;

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

    /** XP earned on a specific calendar day (yyyy-mm-dd). */
    public int getDailyXp(String day) { return dailyXp.getOrDefault(day, 0); }

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
        recordSolveTime(null, millis);
    }

    /**
     * Stores solve-time telemetry for a challenge: fastest, total (for the average)
     * and solve count. Fast solves (<= 30s) also feed the speed-run achievement.
     */
    public void recordSolveTime(String id, long millis) {
        if (millis < 0) millis = 0;
        if (millis <= 30_000) fastSolves++;
        if (id != null) {
            fastestMs.merge(id, millis, Math::min);
            totalMs.merge(id, millis, Long::sum);
            solveTimesCount.merge(id, 1, Integer::sum);
        }
        save();
    }

    /** Fastest recorded solve time (ms) for a challenge, 0 when never solved. */
    public long getFastestMs(String id) { return fastestMs.getOrDefault(id, 0L); }

    /** Average solve time (ms) for a challenge, 0 when never solved. */
    public long getAvgMs(String id) {
        int n = solveTimesCount.getOrDefault(id, 0);
        if (n == 0) return 0;
        return totalMs.getOrDefault(id, 0L) / n;
    }

    /** Best personal time (ms) across every solved challenge, 0 when none yet. */
    public long getPersonalBestMs() {
        long best = Long.MAX_VALUE;
        for (long v : fastestMs.values()) if (v < best) best = v;
        return best == Long.MAX_VALUE ? 0 : best;
    }

    /**
     * Deterministic simulated network best time (ms). Seeded per operator so the
     * record stays stable across sessions while always slightly beating the bots.
     */
    public long getWorldRecordMs() {
        return 120_000 + Math.abs(new Random(("worldrecord|" + operatorId).hashCode()).nextInt() % 180_000);
    }

    /**
     * Spends XP on hints/reveals. Never drops below zero; returns the amount
     * actually spent (0 means the operator could not afford the cost).
     */
    public int spendXp(int amount) {
        if (amount <= 0) return 0;
        int spent = Math.min(amount, totalXp);
        totalXp -= spent;
        save();
        return spent;
    }

    // ----------------------------------------------------------------
    // BONUS AWARDS (multiplayer / tournaments)
    // ----------------------------------------------------------------

    /** Adds XP without counting as a solved challenge (PvP, cups, promotions). */
    public void awardXp(int amount) {
        if (amount <= 0) return;
        totalXp += amount;
        dailyXp.merge(today(), amount, Integer::sum);
        weeklyXp.merge(thisWeek(), amount, Integer::sum);
        monthlyXp.merge(thisMonth(), amount, Integer::sum);
        addActivity("\u26A1 BONUS", "+" + amount + " XP awarded");
        save();
    }

    /** Adds coins to the wallet (matches, cup placements). */
    public void addCoins(int amount) {
        if (amount == 0) return;
        coins += amount;
        save();
    }

    /** Adds certificate points (cup placements, season rewards). */
    public void addCertPoints(int amount) {
        if (amount == 0) return;
        certPoints += amount;
        save();
    }

    // ----------------------------------------------------------------
    // CAREER MODE (item 16)
    // ----------------------------------------------------------------

    /** Highest career rank the operator has officially claimed. */
    public int getCareerRank() { return careerRank; }

    /** Rank index earned by raw XP, regardless of claiming. */
    public int earnedCareerRank() {
        int idx = 0;
        for (int i = 0; i < CAREER_RANKS.length; i++) {
            if (totalXp >= Integer.parseInt(CAREER_RANKS[i][0])) idx = i;
        }
        return idx;
    }

    /** Timeline of promotions as "Rank Name|yyyy-mm-dd" entries. */
    public List<String> getRankHistory() { return new ArrayList<>(rankHistory); }

    /**
     * Claims every rank the operator qualifies for. Awards a promotion bonus
     * (XP + coins), records the history entry, and returns the newly reached
     * rank index, or -1 when there is nothing to promote.
     */
    public int promoteIfEligible() {
        int earned = earnedCareerRank();
        if (earned <= careerRank) return -1;
        careerRank = earned;
        String[] r = CAREER_RANKS[earned];
        rankHistory.add(r[1] + "|" + today());
        int bonusXp = Integer.parseInt(r[0]) / 20;
        int bonusCoins = 10 + careerRank * 5;
        totalXp += bonusXp;
        coins += bonusCoins;
        addActivity("\uD83C\uDFC6 PROMOTION", "Promoted to " + r[1] + " (+" + bonusXp + " XP, +" + bonusCoins + " coins)");
        save();
        return earned;
    }

    // ----------------------------------------------------------------
    // COMPETITIVE HONOURS (items 17/18)
    // ----------------------------------------------------------------

    public int getPvpWins() { return pvpWins; }
    public void addPvpWin() { pvpWins++; save(); }
    public int getTournamentWins() { return tournamentWins; }
    public void addTournamentWin() { tournamentWins++; save(); }

    // ----------------------------------------------------------------
    // PROFILE CUSTOMIZATION (item 20)
    // ----------------------------------------------------------------

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; save(); }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; save(); }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; save(); }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; save(); }

    /** Proficiency (0-100) for a cipher family, derived from module use + solved count. */
    public int proficiency(String family) {
        int solvedN = 0;
        for (String s : solved.keySet()) if (s.contains(family)) solvedN++;
        int uses = moduleUses.getOrDefault(family.toUpperCase(), 0);
        return Math.min(100, solvedN * 12 + uses * 3);
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
            case "MEDIUM"    -> fam = MEDIUM_FAMILIES[rnd().nextInt(MEDIUM_FAMILIES.length)];
            case "HARD"      -> fam = HARD_FAMILIES[rnd().nextInt(HARD_FAMILIES.length)];
            case "EXPERT"    -> fam = EXPERT_FAMILIES[rnd().nextInt(EXPERT_FAMILIES.length)];
            case "NIGHTMARE" -> fam = NIGHTMARE_FAMILIES[rnd().nextInt(NIGHTMARE_FAMILIES.length)];
            case "IMPOSSIBLE" -> fam = IMPOSSIBLE_FAMILIES[rnd().nextInt(IMPOSSIBLE_FAMILIES.length)];
            default          -> fam = EASY_FAMILIES[rnd().nextInt(EASY_FAMILIES.length)];
        }
        Challenge ch = buildChallenge(fam, difficulty);
        generated.put(ch.id, ch);
        save();
        return ch;
    }

    /** Generates a challenge for an explicit family (or "random" to pick one). */
    public Challenge generateChallenge(String difficulty, String family) {
        String fam = family == null || family.isBlank() || family.equals("random")
            ? defaultFamilyFor(difficulty)
            : family;
        Challenge ch = buildChallenge(fam, difficulty);
        generated.put(ch.id, ch);
        save();
        return ch;
    }

    private String defaultFamilyFor(String difficulty) {
        switch (difficulty == null ? "EASY" : difficulty) {
            case "MEDIUM"    -> { return MEDIUM_FAMILIES[rnd().nextInt(MEDIUM_FAMILIES.length)]; }
            case "HARD"      -> { return HARD_FAMILIES[rnd().nextInt(HARD_FAMILIES.length)]; }
            case "EXPERT"    -> { return EXPERT_FAMILIES[rnd().nextInt(EXPERT_FAMILIES.length)]; }
            case "NIGHTMARE" -> { return NIGHTMARE_FAMILIES[rnd().nextInt(NIGHTMARE_FAMILIES.length)]; }
            case "IMPOSSIBLE" -> { return IMPOSSIBLE_FAMILIES[rnd().nextInt(IMPOSSIBLE_FAMILIES.length)]; }
            default          -> { return EASY_FAMILIES[rnd().nextInt(EASY_FAMILIES.length)]; }
        }
    }

    public void removeGenerated(String id) {
        generated.remove(id);
        save();
    }

    // ----------------------------------------------------------------
    // CATEGORY PAGE (ITEM 8/9) + CERTIFICATE METADATA (ITEM 10)
    // ----------------------------------------------------------------

    /** All learning categories with live progress, rendered as modern cards. */
    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        double timeBoost = Math.min(15, getPracticeHours() * 2);
        for (String[] def : CATEGORY_DEFS) {
            String id = def[0];
            String name = def[1];
            String descr = def[2];
            String difficulty = def[3];
            double perSolve = Double.parseDouble(def[5]);
            double cap = Double.parseDouble(def[6]);

            Random r = new Random(("cat|" + operatorId + "|" + id).hashCode());
            double seededBase = 2 + r.nextInt(12);
            double real = cap > 0 ? Math.min(cap, perSolve * completed) : 0;
            double pct = Math.min(100, seededBase + real + timeBoost);

            int totalLessons = 10 + r.nextInt(15);
            int completedLessons = (int) Math.round(pct / 100.0 * totalLessons);
            int remainingLessons = Math.max(0, totalLessons - completedLessons);
            int unlockedLessons = Math.min(totalLessons, completedLessons + Math.max(1, totalLessons / 5));
            int perLessonXp = 90 + r.nextInt(160);
            int perLessonMin = 4 + r.nextInt(12);
            int xp = totalLessons * perLessonXp;
            int estMinutes = totalLessons * perLessonMin;

            list.add(new Category(id, name, descr, difficulty,
                totalLessons, unlockedLessons, completedLessons, remainingLessons,
                xp, estMinutes, pct));
        }
        return list;
    }

    /** Deterministic, human-friendly verification id for a course certificate. */
    public String verificationId(String courseId) {
        String seed = operatorId + "|" + courseId + "|" + today();
        int h = seed.hashCode();
        return "UCF-" + Integer.toHexString(h & 0xFFFFFFF).toUpperCase() + "-" + today().replace("-", "");
    }

    /** Deterministic instructor drawn from the cryptographer hall of fame. */
    public String instructorFor(String courseId) {
        return CRYPTOGRAPHERS[Math.abs((operatorId + "|" + courseId).hashCode()) % CRYPTOGRAPHERS.length];
    }

    /** Certificate status: GRANTED once a course passes 80% completion. */
    public String certificateStatus(Category c) {
        return c.completionPercent >= 80 ? "GRANTED" : "PENDING";
    }

    /** id, name, description, difficulty, module ("" = none), real-progress per solve, cap. */
    private static final String[][] CATEGORY_DEFS = {
        {"cryptography", "Cryptography", "Classical & modern cipher fundamentals.", "Intermediate", "", "2.0", "45"},
        {"aes", "AES", "Advanced Encryption Standard round structure.", "Advanced", "AES", "6.0", "45"},
        {"rsa", "RSA", "Rivest-Shamir-Adleman public-key mathematics.", "Advanced", "RSA", "6.0", "45"},
        {"ecc", "ECC", "Elliptic-curve cryptography over finite fields.", "Elite", "", "0.0", "0"},
        {"hashing", "Hashing", "Hash functions, digests and data integrity.", "Beginner", "HASH", "8.0", "45"},
        {"encoding", "Encoding", "Base64, hex, binary and friends.", "Beginner", "", "2.0", "45"},
        {"steganography", "Steganography", "Hiding data in plain sight.", "Intermediate", "STEGO", "9.0", "45"},
        {"digital-signatures", "Digital Signatures", "Signing and verifying authenticity.", "Advanced", "", "0.0", "0"},
        {"pgp", "PGP", "Pretty Good Privacy email encryption.", "Intermediate", "", "0.0", "0"},
        {"smime", "S/MIME", "Secure/Multipurpose Internet Mail Extensions.", "Advanced", "", "0.0", "0"},
        {"network-security", "Network Security", "Protocols, firewalls and TLS.", "Intermediate", "", "0.0", "0"},
        {"web-security", "Web Security", "OWASP Top 10, XSS and injection.", "Intermediate", "", "0.0", "0"},
        {"reverse-engineering", "Reverse Engineering", "Decompiling and disassembly.", "Expert", "", "1.2", "35"},
        {"binary-exploitation", "Binary Exploitation", "Buffer overflows, ROP and shellcoding.", "Nightmare", "", "1.0", "30"},
        {"forensics", "Forensics", "Evidence recovery and memory analysis.", "Intermediate", "", "0.8", "25"},
        {"osint", "OSINT", "Open-source intelligence gathering.", "Beginner", "", "0.0", "0"},
        {"password-cracking", "Password Cracking", "Hash attacks and cracking discipline.", "Intermediate", "", "0.0", "0"},
        {"malware-analysis", "Malware Analysis", "Reverse malware and sandboxing.", "Elite", "", "0.0", "0"}
    };

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

    // ----------------------------------------------------------------
    // AI MENTOR (ITEM 13) — mistakes, teaching, quizzes, weaknesses
    // ----------------------------------------------------------------

    /** Tracks a wrong answer per cipher family so the mentor can target weaknesses. */
    public void recordWrongFamily(String family) {
        if (family == null || family.isBlank()) return;
        weakFamilies.merge(family, 1, Integer::sum);
        save();
    }

    /** Weakness profile: family name -> wrong-answer count, strongest first. */
    public List<String[]> getWeakFamilies() {
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : weakFamilies.entrySet()) {
            out.add(new String[]{familyName(e.getKey()), String.valueOf(e.getValue()), e.getKey()});
        }
        out.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
        return out;
    }

    /** Mentor explanation of the classic mistakes behind a cipher family. */
    public String explainMistake(String family) {
        String f = family == null ? "" : family;
        return switch (f) {
            case "caesar" -> "Classic mistake: wrapping past Z. \"Z+1\" must wrap to A, not a symbol. Always reduce (index + shift) mod 26.";
            case "vigenere" -> "The key repeats but only over letters \u2014 spaces and symbols do NOT consume a key step. Most solvers miscount and drift.";
            case "xor" -> "XOR is its own inverse: a\u2295k\u2295k = a. A common slip is XORing with a char instead of its numeric code \u2014 compare byte values, not glyphs.";
            case "base64" -> "Base64 output can contain + and / \u2014 copying only the alphanumerics silently corrupts the last bytes. Copy the exact string.";
            case "affine" -> "You must find a\u207b\u00b9 mod 26 first; gcd(a,26) must equal 1 or decryption is impossible. People forget the modular inverse step.";
            case "railfence" -> "The cipher is read per rail, so ciphertext length determines where each rail starts \u2014 compute the zig-zag pattern before reading rows.";
            case "aes" -> "AES needs the exact key AND IV \u2014 both are 16 bytes in 128-bit CBC. Mixing up padding (PKCS5) or using the key as the IV breaks everything silently.";
            case "rsa" -> "Decrypting uses the private exponent d, not e. Compute m = c^d mod n, then encode m back to bytes \u2014 watch for leading zeros on short messages.";
            case "stego" -> "The message hides in the LEAST significant bit. Extracting the wrong bit-plane (e.g. bit 0 vs bit 1) yields garbage that still *looks* like noise.";
            case "hashing" -> "Hashes are one-way \u2014 you cannot \"decrypt\" them. For a short word list, brute force the preimage by hashing every candidate and comparing digests.";
            case "forensics" -> "Files leave residuals. Look for the repeating pattern in the dump \u2014 hidden bytes are often periodic (every 3rd, 4th or 5th byte).";
            case "playfair" -> "Digraphs cannot repeat: 'XX' becomes 'QX'. If a pair falls in the same row/column the replacement rule changes \u2014 practice all three cases.";
            case "hill" -> "The key matrix must be invertible mod 26 (det coprime with 26). Forgetting to compute det\u207b\u00b9 mod 26 before multiplying is the classic failure.";
            case "transposition" -> "Columnar transposition is a pure shuffle \u2014 frequencies are unchanged. If your answer has the same letters but wrong order, your column count is off.";
            default -> "Slow down and check the basic transform first. Encrypt a tiny sample (HELLO) in the lab, then compare your working against the step-by-step trace.";
        };
    }

    /** Mentor lesson bank for teaching concepts on demand. */
    public String teachConcept(String topic) {
        return switch (topic == null ? "" : topic.toLowerCase()) {
            case "aes" -> """
                AES (Advanced Encryption Standard) is a symmetric block cipher.
                It splits data into 16-byte blocks and runs 10 (AES-128), 12 or 14 rounds.
                Each round: SubBytes (S-box substitution) -> ShiftRows (rotate rows)
                -> MixColumns (matrix multiply) -> AddRoundKey (XOR with the round key).
                The same key encrypts and decrypts, so both parties must keep it secret.""";
            case "rsa" -> """
                RSA is asymmetric: a public key encrypts, a private key decrypts.
                1. Pick two primes p, q.  2. n = p*q (modulus).  3. \u03C6 = (p-1)(q-1).
                4. Choose e coprime to \u03C6.  5. d = e\u207b\u00b9 mod \u03C6.
                Public = (n, e), Private = (n, d).  Encrypt: c = m^e mod n.  Decrypt: m = c^d mod n.""";
            case "xor" -> """
                XOR compares bits: 0\u22950=0, 0\u22951=1, 1\u22950=1, 1\u22951=0.
                It is perfectly reversible \u2014 applying the same key twice restores the text.
                That makes XOR a favorite for one-time pads and simple file encryption.""";
            case "hashing" -> """
                A hash is a fixed-length fingerprint of data (SHA-256 = 64 hex chars).
                It is one-way: you cannot recover the input from the digest.
                Changing ONE bit of input avalanches into ~half the output bits changing.
                Uses: integrity checks, password storage, digital signatures.""";
            case "steganography" -> """
                Steganography hides a message INSIDE another medium so nobody suspects it.
                Common trick: embed one bit of the message into the least-significant bit
                of each pixel's colour channel. The image looks identical, but the LSBs spell the secret.""";
            case "caesar" -> """
                The Caesar cipher shifts every letter by a fixed number of positions.
                Encryption: c = (p + shift) mod 26.  Decryption: p = (c - shift) mod 26.
                ROT13 is Caesar with shift 13 \u2014 applying it twice restores the text.""";
            case "vigenere" -> """
                Vigenere uses a repeating keyword. Each letter is shifted by the key's
                matching letter index: c = (p + k) mod 26. A repeated keyword leaks a
                repeating pattern \u2014 that is how cryptanalysts break it (Kasiski test).""";
            case "playfair" -> """
                Playfair encrypts two letters at a time using a 5x5 key square.
                Same-row pairs shift right, same-column pairs shift down, and diagonal
                pairs take the rectangle corners. 'J' is merged into 'I'.""";
            case "hill" -> """
                The Hill cipher encrypts blocks with linear algebra mod 26.
                Plaintext vector P becomes C = K*P mod 26, where K is the key matrix.
                Decryption needs K\u207b\u00b9 mod 26, which exists only when det(K) is coprime with 26.""";
            case "transposition" -> """
                Transposition ciphers REARRANGE letters without changing them.
                Columnar transposition: write plaintext into columns, then read rows.
                Letter frequencies stay identical \u2014 the cipher is a pure permutation.""";
            case "digital signatures" -> """
                A digital signature proves a message is authentic and unmodified.
                Sign: hash the message, then encrypt the digest with your PRIVATE key.
                Verify: decrypt the signature with your PUBLIC key and compare digests.
                Only the holder of the private key can produce a valid signature.""";
            case "base64" -> """
                Base64 encodes binary into 64 printable characters (A-Z a-z 0-9 + /).
                Every 3 bytes become 4 characters; '=' pads the tail.
                It is NOT encryption \u2014 anyone can decode it instantly.""";
            default -> "Choose a topic \u2014 AES, RSA, XOR, Hashing, Steganography, Caesar, Vigenere, Playfair, Hill, Transposition, Digital Signatures or Base64.";
        };
    }

    /** Recommends the next lesson based on weaknesses and unsolved families. */
    public String recommendNextLesson() {
        List<String[]> weak = getWeakFamilies();
        if (!weak.isEmpty()) {
            String fam = weak.get(0)[2];
            return "Your weakest engine is " + familyName(fam) + " (" + weak.get(0)[1]
                + " wrong answers). Open the CRYPTO LAB, run the " + familyName(fam)
                + " simulator on a sample, then try one more " + familyName(fam) + " challenge.";
        }
        if (completed == 0) return "Start with the EASY tier \u2014 Caesar and Hex are the gentlest entry points to the lab.";
        if (completed < 5) return "You are just warming up. Attack ROT13 and Base64 next \u2014 both teach the core skill: invert a transform.";
        if (completed < 10) return "Chain two solves a day to keep your streak alive. Vigenere is the natural MEDIUM graduation step.";
        if (completed < 20) return "Time for keyed ciphers: XOR and Vigenere. Run both in the CRYPTO LAB before attacking them.";
        if (completed < 50) return "Push into Affine and Rail Fence, then take the Certificate Vault's course tracks seriously \u2014 AES and RSA await.";
        return "You are deep in the vault. Explore AES, RSA and Forensics \u2014 the labs make public-key crypto tangible.";
    }

    /** Encouragement line tuned to current progress and streak. */
    public String encourage() {
        if (completed == 0) return "Every master once started at zero. Open the lab, encrypt HELLO with Caesar, and feel the power of the first win \u2014 I'll be right here.";
        if (streak >= 7) return "A seven-day streak \u2014 that is elite discipline. The ciphertext is afraid of you now.";
        if (completed >= 100) return "Over 100 ciphers cleared. You are not a student anymore \u2014 you are a cryptanalyst. The network respects you.";
        if (completed >= 20) return "Twenty-plus ciphers down. Momentum is your weapon \u2014 keep the streak alive and the badges will follow.";
        return "Every solve sharpens the blade. One more challenge today and you move up the global leaderboard \u2014 I believe in you, operator.";
    }

    // ---- Quiz engine (deterministic, seeded per operator/day) ----

    private static final String[][] QUIZ = {
        {"Which cipher uses the least-significant bit of pixels to hide data?", "AES", "Steganography", "Base64", "Rail Fence", "1", "Stego hides data in media so it looks innocent \u2014 e.g. message bits in the LSB of pixel colours."},
        {"In AES-128, how many rounds does each 16-byte block pass through?", "8", "10", "12", "16", "1", "AES-128 uses 10 rounds, AES-192 uses 12, and AES-256 uses 14."},
        {"What does the private key d do in RSA?", "Encrypts", "Decrypts", "Hashes", "Pads", "1", "m = c^d mod n. Only the holder of d can decrypt a message encrypted with the public key (n, e)."},
        {"Why can a hash not be 'decrypted' back to its input?", "It is too short", "It is one-way", "It is encrypted", "It uses a key", "1", "A digest is a lossy, one-way fingerprint \u2014 many inputs map to the same digest; you brute-force, not reverse."},
        {"XOR is its own ______.", "enemy", "inverse", "padding", "salt", "1", "Applying the same key twice restores the original: a\u2295k\u2295k = a."},
        {"A 'D' shifts 4 steps backward under Caesar to?", "A", "Z", "Y", "B", "1", "D(3) - 4 = -1 -> wrap to 25 = Z."},
        {"What is Base64 padding for?", "Alignment", "Trailing bytes", "Encryption strength", "Keys", "1", "'=' pads the final group so it fills 3 bytes -> 4 characters."},
        {"Which attack exploits a repeating Vigenere key?", "Kasiski", "Side-channel", "Replay", "Birthday", "0", "Repeating keys leak periodic patterns \u2014 the Kasiski examination finds the key length."},
        {"The one-time pad is only unbreakable if the key is ______.", "short", "truly random", "reused", "a word", "1", "A random key as long as the message, never reused, makes the OTP information-theoretically secure."},
        {"Hill cipher works with which maths?", "Linear algebra mod 26", "Group theory", "Set theory", "Calculus", "0", "C = K*P mod 26 needs an invertible key matrix (det coprime to 26)."},
        {"What does a digital signature protect against?", "Loss", "Tampering", "Size", "Speed", "1", "Signing the digest with your private key proves authenticity and integrity \u2014 tampering invalidates the signature."},
        {"Which is NOT a symmetric cipher?", "AES", "DES", "RSA", "ChaCha20", "2", "RSA is asymmetric (public/private key); AES, DES and ChaCha20 are symmetric."},
        {"The Caesar cipher is a special case of which larger family?", "Vigenere", "Playfair", "Affine", "Hill", "2", "Caesar is Affine with a=1: c = (1*p + b) mod 26."},
        {"Rail fence encryption is a form of ______.", "substitution", "transposition", "hashing", "padding", "1", "It rearranges letters in a zig-zag \u2014 no letter changes, only positions."},
        {"Forensics: hidden bytes are often placed ______ in a dump.", "randomly", "periodically", "as keys", "encrypted", "1", "Steganographic/forensic markers commonly repeat at a fixed interval (every 3rd byte, etc.)."}
    };

    private static final int QUIZ_PER_DAY = 5;

    private int[] quizSelection;
    private int[] quizOrder;

    /** Deterministic quiz for today: indices into QUIZ, plus per-question shuffled option order. */
    public void generateQuiz() {
        Random r = new Random((operatorId + "|quiz|" + today()).hashCode());
        int[] sel = new int[QUIZ_PER_DAY];
        java.util.Set<Integer> used = new HashSet<>();
        int i = 0;
        while (i < QUIZ_PER_DAY) {
            int idx = r.nextInt(QUIZ.length);
            if (used.add(idx)) sel[i++] = idx;
        }
        quizSelection = sel;
        quizOrder = new int[QUIZ_PER_DAY];
        for (int q = 0; q < QUIZ_PER_DAY; q++) {
            quizOrder[q] = 1 + r.nextInt(4);
        }
    }

    public int quizCount() { return QUIZ_PER_DAY; }

    public String quizQuestion(int i) { return QUIZ[quizSelection[i]][0]; }

    /** Returns options in display order for question i; returns correct index in that order. */
    public String[] quizOptions(int i) {
        String[] src = new String[]{QUIZ[quizSelection[i]][1], QUIZ[quizSelection[i]][2],
            QUIZ[quizSelection[i]][3], QUIZ[quizSelection[i]][4]};
        int correct = Integer.parseInt(QUIZ[quizSelection[i]][5]);
        String[] out = new String[4];
        int place = quizOrder[i] % 4;
        out[place] = src[correct];
        int idx = 0;
        for (int k = 0; k < 4; k++) {
            if (k == place) continue;
            while (idx == correct) idx++;
            out[k] = src[idx];
            idx++;
        }
        return out;
    }

    /** Which display position holds the correct answer for question i. */
    public int quizAnswerIndex(int i) { return quizOrder[i] % 4; }

    public String quizExplain(int i) { return QUIZ[quizSelection[i]][6]; }

    // ----------------------------------------------------------------
    // SHARED CRYPTO TOOLKIT (used by generator + interactive labs)
    // ----------------------------------------------------------------

    /** AES-128-CBC encryption of plaintext, base64 key + iv, returns base64 ciphertext. */
    public static String aesEncryptB64(String plain, String keyB64, String ivB64) {
        try {
            byte[] key = java.util.Base64.getDecoder().decode(keyB64);
            byte[] iv = java.util.Base64.getDecoder().decode(ivB64);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return java.util.Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "[AES ERROR: " + e.getMessage() + "]";
        }
    }

    /** AES-128-CBC decryption; returns plaintext or an error tag. */
    public static String aesDecryptB64(String cipherB64, String keyB64, String ivB64) {
        try {
            byte[] key = java.util.Base64.getDecoder().decode(keyB64);
            byte[] iv = java.util.Base64.getDecoder().decode(ivB64);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] out = c.doFinal(java.util.Base64.getDecoder().decode(cipherB64.trim()));
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[AES ERROR: " + e.getMessage() + "]";
        }
    }

    public static String aesRandomKeyB64(Random r) {
        byte[] k = new byte[16];
        new SecureRandom().nextBytes(k);
        return java.util.Base64.getEncoder().encodeToString(k);
    }

    public static String aesRandomIvB64(Random r) {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return java.util.Base64.getEncoder().encodeToString(iv);
    }

    /** SHA-256 hex digest. */
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "[HASH ERROR]";
        }
    }

    public static long modInverse(long a, long m) {
        return BigInteger.valueOf(a).modInverse(BigInteger.valueOf(m)).longValue();
    }

    /** Encodes a word into a number: A=10..Z=35 so leading zeros never collapse. */
    public static long wordToNumber(String word) {
        long m = 0;
        for (char ch : word.toCharArray()) {
            m = m * 100 + (ch - 'A' + 10);
        }
        return m;
    }

    public static String numberToWord(long m) {
        String s = String.valueOf(m);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < s.length(); i += 2) {
            int v = Integer.parseInt(s.substring(i, i + 2));
            if (v >= 10 && v <= 35) sb.append((char) ('A' + v - 10));
        }
        return sb.toString();
    }

    /** Embeds the message (length-prefixed) into a fake pixel grid, one bit per green LSB. */
    public static String stegoEmbed(String msg) {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[data.length + 1];
        frame[0] = (byte) data.length;
        System.arraycopy(data, 0, frame, 1, data.length);
        int totalBits = frame.length * 8;
        int cells = Math.max(16, ((totalBits + 15) / 16) * 16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells; i++) {
            int base = 120 + (i % 9) * 10;      // plausible green channel value
            int bit = 0;
            int byteIdx = i / 8;
            int bitIdx = 7 - (i % 8);
            if (byteIdx < frame.length) bit = (frame[byteIdx] >> bitIdx) & 1;
            int hidden = (base & 0xFE) | bit;
            sb.append(String.format("%03d", hidden));
            if (i % 16 == 15) sb.append("\n"); else sb.append(" ");
        }
        return sb.toString();
    }

    /** Extracts the length-prefixed message from the green-LSB pixel grid. */
    public static String stegoExtract(String grid) {
        StringBuilder bits = new StringBuilder();
        String[] nums = grid.replace("\n", " ").trim().split("\\s+");
        for (String num : nums) {
            try {
                bits.append((char) ('0' + (Integer.parseInt(num) & 1)));
            } catch (Exception ignored) { }
        }
        if (bits.length() < 8) return "";
        int len = 0;
        for (int j = 0; j < 8; j++) len = (len << 1) | (bits.charAt(j) - '0');
        StringBuilder out = new StringBuilder();
        for (int i = 8; i + 8 <= bits.length() && out.length() < len; i += 8) {
            int v = 0;
            for (int j = 0; j < 8; j++) v = (v << 1) | (bits.charAt(i + j) - '0');
            out.append((char) v);
        }
        return out.toString();
    }

    /** Hides the flag as every 3rd byte of a fake file dump. */
    public static String forensicsHide(String msg) {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length * 3; i++) {
            int b;
            if (i % 3 == 0) {
                b = data[i / 3] & 0xFF;
            } else {
                b = 0x40 + (i * 7) % 120; // plausible file noise
            }
            sb.append(String.format("%02X", b));
            if (i % 16 == 15) sb.append("\n"); else sb.append(" ");
        }
        return sb.toString();
    }

    /** Extracts every 3rd byte (starts at index 0) and decodes ASCII. */
    public static String forensicsExtract(String dump) {
        String hex = dump.replace("\n", " ").replace(" ", "");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < hex.length(); i += 6) {
            String pair = hex.substring(i, i + 2);
            out.append((char) Integer.parseInt(pair, 16));
        }
        return out.toString();
    }

    /** Playfair encryption (I/J merged). */
    public static String playfairEncrypt(String plain, String key) {
        char[][] sq = playfairSquare(key);
        String clean = plain.toUpperCase().replace("J", "I").replaceAll("[^A-Z]", "");
        StringBuilder pairs = new StringBuilder();
        for (int i = 0; i < clean.length(); i += 2) {
            char a = clean.charAt(i);
            char b = (i + 1 < clean.length()) ? clean.charAt(i + 1) : 'X';
            if (a == b) { b = 'X'; i--; }
            pairs.append(a).append(b);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length(); i += 2) {
            char a = pairs.charAt(i), b = pairs.charAt(i + 1);
            int[] pa = findPlayfair(sq, a), pb = findPlayfair(sq, b);
            char ea, eb;
            if (pa[0] == pb[0]) { // same row
                ea = sq[pa[0]][(pa[1] + 1) % 5];
                eb = sq[pb[0]][(pb[1] + 1) % 5];
            } else if (pa[1] == pb[1]) { // same column
                ea = sq[(pa[0] + 1) % 5][pa[1]];
                eb = sq[(pb[0] + 1) % 5][pb[1]];
            } else {
                ea = sq[pa[0]][pb[1]];
                eb = sq[pb[0]][pa[1]];
            }
            out.append(ea).append(eb);
        }
        return out.toString();
    }

    private static char[][] playfairSquare(String key) {
        String k = (key.toUpperCase() + "ABCDEFGHIKLMNOPQRSTUVWXYZ").replace("J", "I");
        LinkedHashMap<Character, Boolean> seen = new LinkedHashMap<>();
        for (char c : k.toCharArray()) if (c >= 'A' && c <= 'Z') seen.put(c, true);
        char[][] sq = new char[5][5];
        int i = 0;
        for (char c : seen.keySet()) { sq[i / 5][i % 5] = c; i++; }
        return sq;
    }

    private static int[] findPlayfair(char[][] sq, char c) {
        for (int r = 0; r < 5; r++) for (int col = 0; col < 5; col++)
            if (sq[r][col] == c) return new int[]{r, col};
        return new int[]{0, 0};
    }

    /** Hill cipher 2x2 encryption, key letters a,b,c,d. */
    public static String hillEncrypt(String plain, String key) {
        String k = key.toUpperCase().replaceAll("[^A-Z]", "");
        if (k.length() < 4) k = "GYBN";
        int[][] K = {
            {k.charAt(0) - 'A', k.charAt(1) - 'A'},
            {k.charAt(2) - 'A', k.charAt(3) - 'A'}
        };
        String p = plain.toUpperCase().replaceAll("[^A-Z]", "");
        if (p.length() % 2 != 0) p += "X";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < p.length(); i += 2) {
            int x = p.charAt(i) - 'A', y = p.charAt(i + 1) - 'A';
            int c1 = mod26(K[0][0] * x + K[0][1] * y);
            int c2 = mod26(K[1][0] * x + K[1][1] * y);
            out.append((char) ('A' + c1)).append((char) ('A' + c2));
        }
        return out.toString();
    }

    private static int mod26(int v) { return ((v % 26) + 26) % 26; }

    /** Columnar transposition: pad, write columns, read rows. */
    public static String transpositionEncrypt(String plain, int cols) {
        String p = plain.toUpperCase().replaceAll("[^A-Z]", "");
        int rows = (p.length() + cols - 1) / cols;
        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) grid[r][c] = 'X';
        int idx = 0;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++)
            if (idx < p.length()) grid[r][c] = p.charAt(idx++);
        StringBuilder out = new StringBuilder();
        for (int c = 0; c < cols; c++) for (int r = 0; r < rows; r++)
            out.append(grid[r][c]);
        return out.toString();
    }

    /** Columnar transposition decryption. */
    public static String transpositionDecrypt(String cipher, int cols) {
        String c = cipher.toUpperCase().replaceAll("[^A-Z]", "");
        int rows = (c.length() + cols - 1) / cols;
        char[][] grid = new char[rows][cols];
        int idx = 0;
        for (int col = 0; col < cols; col++) for (int r = 0; r < rows; r++)
            if (idx < c.length()) grid[r][col] = c.charAt(idx++);
        StringBuilder out = new StringBuilder();
        for (int r = 0; r < rows; r++) for (int col = 0; col < cols; col++)
            out.append(grid[r][col]);
        return out.toString();
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
        weakFamilies.clear();
        fastestMs.clear();
        totalMs.clear();
        solveTimesCount.clear();
        careerRank = 0;
        rankHistory.clear();
        pvpWins = 0;
        tournamentWins = 0;
        avatar = "\uD83D\uDD75\uFE0F";
        bio = "Cryptographic operator in training.";
        country = COUNTRIES[0];
        university = "";
        notifications.clear();
        settings.clear();
        storedSig = "";
        sigOk = true;
        scanPassed = -1;
        standingsCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
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
            apply(j);
            storedSig = j.optString("sig", "");
            sigOk = storedSig.isEmpty() || storedSig.equals(computeSignature());
        } catch (Exception e) {
            System.err.println("[ACADEMY] Profile load failed (safe ignore): " + e.getMessage());
        }
    }

    /** Applies a loaded profile JSON onto this instance (shared by load + backup restore). */
    private void apply(JSONObject j) {
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
        readBucket(j, "weakFamilies", weakFamilies);
        readBucketLong(j, "fastestMs", fastestMs);
        readBucketLong(j, "totalMs", totalMs);
        readBucket(j, "solveTimesCount", solveTimesCount);
        readBucket(j, "dailyXp", dailyXp);
        readBucket(j, "weeklyXp", weeklyXp);
        readBucket(j, "monthlyXp", monthlyXp);
        careerRank = j.optInt("careerRank");
        JSONArray rh = j.optJSONArray("rankHistory");
        if (rh != null) for (int i = 0; i < rh.length(); i++) rankHistory.add(rh.getString(i));
        pvpWins = j.optInt("pvpWins");
        tournamentWins = j.optInt("tournamentWins");
        avatar = j.optString("avatar", avatar);
        bio = j.optString("bio", bio);
        country = j.optString("country", country);
        university = j.optString("university", university);
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
        JSONObject st = j.optJSONObject("settings");
        if (st != null) for (String k : st.keySet()) settings.put(k, st.optString(k));
        JSONArray nots = j.optJSONArray("notifications");
        if (nots != null) {
            for (int i = 0; i < nots.length(); i++) {
                JSONObject n = nots.getJSONObject(i);
                notifications.addLast(new Notif(
                    n.optString("type"), n.optString("title"), n.optString("detail"),
                    n.optLong("ts"), n.optBoolean("read", false)));
            }
        }
    }

    /** Serializes current state to a profile JSON (shared by save + encrypted backup). */
    private JSONObject toJson() {
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
        writeBucket(j, "weakFamilies", weakFamilies);
        writeBucketLong(j, "fastestMs", fastestMs);
        writeBucketLong(j, "totalMs", totalMs);
        writeBucket(j, "solveTimesCount", solveTimesCount);
        writeBucket(j, "dailyXp", dailyXp);
        writeBucket(j, "weeklyXp", weeklyXp);
        writeBucket(j, "monthlyXp", monthlyXp);
        j.put("careerRank", careerRank);
        JSONArray rh = new JSONArray();
        for (String h : rankHistory) rh.put(h);
        j.put("rankHistory", rh);
        j.put("pvpWins", pvpWins);
        j.put("tournamentWins", tournamentWins);
        j.put("avatar", avatar);
        j.put("bio", bio);
        j.put("country", country);
        j.put("university", university);
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
        JSONObject st = new JSONObject();
        for (Map.Entry<String, String> e : settings.entrySet()) st.put(e.getKey(), e.getValue());
        j.put("settings", st);
        JSONArray nots = new JSONArray();
        for (Notif n : notifications) {
            JSONObject no = new JSONObject();
            no.put("type", n.type); no.put("title", n.title); no.put("detail", n.detail);
            no.put("ts", n.ts); no.put("read", n.read);
            nots.put(no);
        }
        j.put("notifications", nots);
        return j;
    }

    public void save() {
        try {
            JSONObject j = toJson();
            j.put("sig", computeSignature());
            writeProfile(j);
        } catch (Exception e) {
            System.err.println("[ACADEMY] Profile save failed (safe ignore): " + e.getMessage());
        }
    }

    private void writeProfile(JSONObject j) throws Exception {
        File dir = new File(APP_DIR);
        if (!dir.exists()) dir.mkdirs();
        Files.writeString(new File(PROFILE_FILE).toPath(), j.toString(2));
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

    private static void readBucketLong(JSONObject j, String key, Map<String, Long> out) {
        JSONObject o = j.optJSONObject(key);
        if (o == null) return;
        for (String k : o.keySet()) out.put(k, o.optLong(k));
    }

    private static void writeBucketLong(JSONObject j, String key, Map<String, Long> in) {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Long> e : in.entrySet()) o.put(e.getKey(), e.getValue());
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
            case "aes" -> {
                String key = aesRandomKeyB64(r);
                String iv = aesRandomIvB64(r);
                String ct = aesEncryptB64(flag, key, iv);
                stars = 4; xp = 420;
                descr = "AES-128-CBC decrypt (PKCS5): " + ct
                    + "\nKEY=" + key + "  IV=" + iv;
                hint = "Paste the ciphertext into the AES DECRYPT lab with the key and IV above.";
            }
            case "rsa" -> {
                int[] pq = {61, 53};
                long n = (long) pq[0] * pq[1];
                long phi = (long) (pq[0] - 1) * (pq[1] - 1);
                long e = 17;
                long d = modInverse(e, phi);
                long m = wordToNumber(word);
                long c = BigInteger.valueOf(m).modPow(BigInteger.valueOf(e), BigInteger.valueOf(n)).longValue();
                stars = 5; xp = 500;
                descr = "RSA decrypt. n=" + n + ", e=" + e + ", c=" + c
                    + "\nRecover m, then map digits back to letters (A=10,B=11...).";
                hint = "Private exponent d=" + d + ". Decrypt with the RSA lab: c^d mod n.";
            }
            case "stego" -> {
                String matrix = stegoEmbed(flag);
                stars = 5; xp = 480;
                descr = "An image hides the flag in the lowest bit of every green channel.\nPixel grid:\n" + matrix;
                hint = "Extract the LSB of each green value with the STEGANOGRAPHY lab.";
            }
            case "hashing" -> {
                String target = sha256Hex(flag);
                String[] options = new String[5];
                options[0] = word;
                for (int i = 1; i < 5; i++) options[i] = WORDS[r.nextInt(WORDS.length)];
                java.util.Arrays.sort(options);
                stars = 4; xp = 380;
                descr = "SHA-256(" + flag + ") = " + target
                    + "\nWhich of these words is the preimage?  " + String.join(", ", options);
                hint = "Run each candidate through the HASH lab until the digest matches.";
            }
            case "forensics" -> {
                String dump = forensicsHide(flag);
                stars = 5; xp = 520;
                descr = "Recover the deleted file. The flag bytes survive as every 3rd byte of this hex dump:\n" + dump;
                hint = "Take every 3rd byte (starting at index 0) and decode to ASCII with the FORENSICS lab.";
            }
            case "playfair" -> {
                String key = WORDS[r.nextInt(WORDS.length)];
                stars = 3; xp = 260;
                descr = "Playfair with key '" + key + "' encrypts the flag word as: " + playfairEncrypt(word, key);
                hint = "Build the 5x5 key square, then decrypt the digraph pairs with the PLAYFAIR lab.";
            }
            case "hill" -> {
                String key = "GYBN";
                stars = 4; xp = 340;
                descr = "Hill cipher (2x2 key GYBN -> [[6,24],[1,13]], mod 26) encrypts: " + hillEncrypt(word, key);
                hint = "Use the HILL lab: invert the key matrix mod 26 and decrypt the digraphs.";
            }
            case "transposition" -> {
                int cols = 2 + r.nextInt(Math.max(2, word.length() - 2));
                stars = 3; xp = 250;
                descr = "Columnar transposition (" + cols + " columns): " + transpositionEncrypt(word, cols);
                hint = "Write the ciphertext row-by-row into " + cols + " columns, then read each column top-to-bottom to recover the word.";
            }
            default -> {
                stars = 1; xp = 100;
                descr = fam + ": " + word;
                hint = "Analyze the cipher and reverse it.";
            }
        }

        switch (diff) {
            case "EXPERT" -> { stars += 2; xp *= 2; }
            case "NIGHTMARE" -> { stars += 3; xp *= 3; }
            case "IMPOSSIBLE" -> { stars += 4; xp *= 4; }
            default -> { }
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
            case "aes" -> "AES Block Cipher";
            case "rsa" -> "RSA Public-Key";
            case "stego" -> "Steganography";
            case "hashing" -> "Hashing (SHA-256)";
            case "forensics" -> "Digital Forensics";
            case "playfair" -> "Playfair Cipher";
            case "hill" -> "Hill Cipher";
            case "transposition" -> "Columnar Transposition";
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

    /** A learning category with its live progress tracker stats. */
    public static final class Category {
        public final String id, name, descr, difficulty;
        public final int totalLessons, unlockedLessons, completedLessons, remainingLessons;
        public final int xp, estMinutes;
        public final double completionPercent;
        public Category(String id, String name, String descr, String difficulty,
                        int totalLessons, int unlockedLessons, int completedLessons, int remainingLessons,
                        int xp, int estMinutes, double completionPercent) {
            this.id = id; this.name = name; this.descr = descr; this.difficulty = difficulty;
            this.totalLessons = totalLessons; this.unlockedLessons = unlockedLessons;
            this.completedLessons = completedLessons; this.remainingLessons = remainingLessons;
            this.xp = xp; this.estMinutes = estMinutes; this.completionPercent = completionPercent;
        }
    }

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

    /** All selectable avatar glyphs for the profile page. */
    public static String[] avatars() { return AVATARS.clone(); }

    /** All selectable countries for the profile page. */
    public static String[] countries() { return COUNTRIES.clone(); }

    /** All selectable universities for the profile page. */
    public static String[] universities() { return UNIVERSITIES.clone(); }

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

    /** Top-N standings for the global leaderboard view (cached with a 30s TTL). */
    public List<Standing> getGlobalStandings(int topN) {
        return cachedStandings("global|" + topN, () -> {
            List<Bot> all = new ArrayList<>(bots());
            all.add(new Bot(operatorId, myCountry(), totalXp));
            return ranked(topN, all);
        });
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

    // ----------------------------------------------------------------
    // NOTIFICATIONS (item 21)
    // ----------------------------------------------------------------

    /** A single in-app notification. */
    public static final class Notif {
        public final String type;
        public final String title;
        public final String detail;
        public final long ts;
        public boolean read;
        public Notif(String type, String title, String detail, long ts, boolean read) {
            this.type = type; this.title = title; this.detail = detail; this.ts = ts; this.read = read;
        }
    }

    public List<Notif> getNotifications() { return new ArrayList<>(notifications); }

    public int unreadCount() {
        int n = 0;
        for (Notif t : notifications) if (!t.read) n++;
        return n;
    }

    /** Appends a notification (most recent first) and persists. */
    public void notify(String type, String title, String detail) {
        notifications.addFirst(new Notif(type, title, detail, System.currentTimeMillis(), false));
        while (notifications.size() > MAX_NOTIFS) notifications.removeLast();
        save();
    }

    /** Adds a notification only if no identical unread entry already exists (dedupe for repeat events). */
    public void notifyOnce(String type, String title, String detail) {
        for (Notif n : notifications) {
            if (!n.read && n.title.equals(title) && n.type.equals(type)) return;
        }
        notify(type, title, detail);
    }

    public void markAllNotificationsRead() {
        boolean any = false;
        for (Notif n : notifications) if (!n.read) { n.read = true; any = true; }
        if (any) save();
    }

    // ----------------------------------------------------------------
    // SETTINGS (item 23)
    // ----------------------------------------------------------------

    public String getSetting(String key, String def) { return settings.getOrDefault(key, def); }

    public void setSetting(String key, String value) {
        settings.put(key, value);
        save();
    }

    public Map<String, String> getSettings() { return new LinkedHashMap<>(settings); }

    /** Convenience: true unless the setting is explicitly "off". */
    public boolean settingOn(String key) { return !"off".equalsIgnoreCase(getSetting(key, "on")); }

    // ----------------------------------------------------------------
    // SECURITY (item 24): anti-tamper signature, integrity scan, encrypted backup
    // ----------------------------------------------------------------

    /** Deterministic signature over the cheat-relevant profile fields. */
    private String computeSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(operatorId).append('|')
          .append(totalXp).append('|').append(coins).append('|').append(certPoints).append('|')
          .append(careerRank).append('|').append(pvpWins).append('|').append(tournamentWins).append('|');
        java.util.TreeMap<String, Integer> sorted = new java.util.TreeMap<>(solved);
        for (Map.Entry<String, Integer> e : sorted.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append(';');
        return sha256Hex(sb.toString() + SIG_SALT);
    }

    public boolean isProfileIntegrityOk() { return sigOk; }

    /** True once a stored signature is present in the profile file (i.e. it has been signed). */
    public boolean isProfileSigned() { return !storedSig.isEmpty(); }

    public long getLastScan() { return lastScan; }

    /** 1 = all checks passed, 0 = at least one failed, -1 = scan not run yet. */
    public int lastScanPassed() { return scanPassed; }

    /** One row of an integrity scan report. */
    public static final class ScanResult {
        public final String check;
        public final boolean ok;
        public final String detail;
        public ScanResult(String check, boolean ok, String detail) {
            this.check = check; this.ok = ok; this.detail = detail;
        }
    }

    /** Runs a full anti-tamper / integrity audit over the saved profile. */
    public List<ScanResult> integrityScan() {
        List<ScanResult> out = new ArrayList<>();
        out.add(new ScanResult("Profile integrity signature", sigOk,
            sigOk ? (storedSig.isEmpty() ? "unsigned profile \u2014 will sign on next save" : "signed & verified") : "FILE TAMPERED"));
        boolean pos = true;
        for (Integer v : solved.values()) if (v == null || v <= 0) pos = false;
        out.add(new ScanResult("Solve XP values positive", pos, solved.size() + " solved entries"));
        boolean flagsOk = true;
        Set<String> ids = new HashSet<>();
        boolean dup = false;
        for (Challenge c : generated.values()) {
            if (c.flag == null || !c.flag.startsWith("UC{")) flagsOk = false;
            if (!ids.add(c.id)) dup = true;
        }
        out.add(new ScanResult("Challenge flag format (UC{...})", flagsOk, generated.size() + " generated challenges"));
        out.add(new ScanResult("Unique challenge IDs", !dup, ids.size() + " unique ids"));
        boolean nonNeg = coins >= 0 && certPoints >= 0 && totalXp >= 0 && practiceSeconds >= 0;
        out.add(new ScanResult("Currency / XP / time non-negative", nonNeg, "coins=" + coins + " certPts=" + certPoints));
        boolean rankOk = careerRank >= 0 && careerRank < CAREER_RANKS.length;
        out.add(new ScanResult("Career rank within bounds", rankOk, careerRank + " / " + (CAREER_RANKS.length - 1)));
        out.add(new ScanResult("Cheat-prevention gate", settingOn("cheatPrevention"), "XP edits only via awardXp()"));
        out.add(new ScanResult("Challenge validation", settingOn("challengeValidation"), "flags validated server-side pattern"));
        out.add(new ScanResult("Secure profile storage", settingOn("profileEncryption"), "AES-encrypted backups available"));
        lastScan = System.currentTimeMillis();
        int passed = 0;
        for (ScanResult r : out) if (r.ok) passed++;
        scanPassed = passed == out.size() ? 1 : 0;
        return out;
    }

    private String keyFromPassword(String password) {
        String digest = sha256Hex(password == null || password.isEmpty() ? "ucsuite-default" : password);
        return java.util.Base64.getEncoder().encodeToString(digest.substring(0, 32).getBytes(StandardCharsets.UTF_8));
    }

    private static final String BACKUP_IV = "dXNlci1iYWNrdXAtaXYxNg==";

    /** Writes an AES-encrypted snapshot of the profile to ~/.ucsuite/backups/. Returns the file path. */
    public String createEncryptedBackup(String password) {
        try {
            JSONObject j = toJson();
            j.put("sig", computeSignature());
            String enc = aesEncryptB64(j.toString(2), keyFromPassword(password), BACKUP_IV);
            File dir = new File(APP_DIR, "backups");
            if (!dir.exists()) dir.mkdirs();
            String path = dir + File.separator + "backup_" + System.currentTimeMillis() + ".ucb";
            Files.writeString(new File(path).toPath(), enc);
            return path;
        } catch (Exception e) {
            System.err.println("[ACADEMY] Backup failed: " + e.getMessage());
            return null;
        }
    }

    /** Restores a profile from an encrypted backup file. Returns true on success. */
    public boolean restoreEncryptedBackup(String path, String password) {
        try {
            String enc = Files.readString(new File(path).toPath());
            String plain = aesDecryptB64(enc, keyFromPassword(password), BACKUP_IV);
            if (plain == null || plain.startsWith("[AES ERROR")) return false;
            JSONObject j = new JSONObject(plain);
            if (!operatorId.equals(j.optString("operatorId", operatorId))) return false;
            apply(j);
            storedSig = j.optString("sig", "");
            sigOk = storedSig.isEmpty() || storedSig.equals(computeSignature());
            save();
            return true;
        } catch (Exception e) {
            System.err.println("[ACADEMY] Restore failed: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------
    // PERFORMANCE (item 25): standings cache with TTL + counters
    // ----------------------------------------------------------------

    private List<Standing> cachedStandings(String key, java.util.function.Supplier<List<Standing>> supplier) {
        CacheEntry entry = standingsCache.get(key);
        long now = System.currentTimeMillis();
        if (entry != null && entry.ts + CACHE_TTL_MS > now) {
            cacheHits++;
            return entry.rows;
        }
        cacheMisses++;
        List<Standing> list = supplier.get();
        standingsCache.put(key, new CacheEntry(list, now));
        if (standingsCache.size() > 8) standingsCache.keySet().iterator().remove();
        return list;
    }

    private static final class CacheEntry {
        final List<Standing> rows;
        final long ts;
        CacheEntry(List<Standing> rows, long ts) { this.rows = rows; this.ts = ts; }
    }

    public long getCacheHits() { return cacheHits; }
    public long getCacheMisses() { return cacheMisses; }
    public int getCacheSize() { return standingsCache.size(); }
    public int getCachedChallenges() { return generated.size(); }

    public void clearCaches() {
        standingsCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }

    public long usedMemoryBytes() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long maxMemoryBytes() { return Runtime.getRuntime().maxMemory(); }
}
