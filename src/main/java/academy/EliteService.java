package academy;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * UC-FORTRESS ACADEMY ELITE EDITION - enterprise capability service.
 *
 * <p>Owns the ten elite modes: Learning Path Engine, Cyber Range, Mission Mode,
 * Story Mode, Team Mode, Mentor Mode, University Mode, Company Training Mode,
 * Exam Mode and Lab Builder. Progress is persisted to
 * {@code ~/.ucsuite/elite_profile.json}, a sibling of the core profile so the
 * core integrity signature is never disturbed.</p>
 *
 * <p>Rewards (XP / coins / cert points / notifications) are funnelled through
 * the injected {@link AcademyService} so the main profile stays the single
 * source of truth for currency and achievements.</p>
 */
public class EliteService {

    private static final String APP_DIR = System.getProperty("user.home") + File.separator + ".ucsuite";
    private static final String FILE = APP_DIR + File.separator + "elite_profile.json";

    private final String operatorId;
    private final AcademyService academy;
    private final Random rnd = new Random();

    // ----- Learning Path Engine -----
    private final Map<String, PathProgress> pathProgress = new LinkedHashMap<>();
    private final Set<String> pathCompleted = new LinkedHashSet<>();

    // ----- Cyber Range -----
    private final Map<String, Integer> rangeStepsDone = new LinkedHashMap<>();
    private final Set<String> rangeDone = new LinkedHashSet<>();

    // ----- Mission Mode -----
    private final Set<String> missionDone = new LinkedHashSet<>();
    private final Map<String, Integer> missionObjectiveProgress = new LinkedHashMap<>();
    private final Map<String, String> missionReports = new LinkedHashMap<>();

    // ----- Story Mode -----
    private final Set<String> storyDone = new LinkedHashSet<>();
    private final Map<String, Integer> storySceneProgress = new LinkedHashMap<>();

    // ----- Team Mode -----
    private final Map<String, Team> teams = new LinkedHashMap<>();

    // ----- Mentor Mode -----
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, StudentRecord> students = new LinkedHashMap<>();

    // ----- University Mode -----
    private final Map<String, Department> departments = new LinkedHashMap<>();

    // ----- Company Training Mode -----
    private final Map<String, Company> companies = new LinkedHashMap<>();

    // ----- Exam Mode -----
    private final Map<String, ExamResult> examResults = new LinkedHashMap<>();
    private Exam currentExam;

    // ----- Lab Builder -----
    private final Map<String, CustomLab> customLabs = new LinkedHashMap<>();

    // ----- Custom CTF Builder -----
    private final Map<String, CustomCtf> customCtfs = new LinkedHashMap<>();

    // ----- AI Curriculum Engine -----
    private final Map<String, Double> topicMastery = new LinkedHashMap<>();
    private final Map<String, Integer> topicAttempts = new LinkedHashMap<>();
    private final Map<String, Integer> topicCorrect = new LinkedHashMap<>();

    // ----- Digital Notebook -----
    private final Map<String, NotebookEntry> notebookEntries = new LinkedHashMap<>();

    // ----- Portfolio -----
    private final Map<String, PortfolioItem> portfolioItems = new LinkedHashMap<>();
    private String githubUrl = "";
    private String linkedinUrl = "";

    // ----- Reputation System -----
    private int trustPoints, reputationPoints, communityPoints, mentorRating, contributorRating;

    // ----- Community Hub -----
    private final Map<String, Board> boards = new LinkedHashMap<>();
    private final Map<String, Writeup> writeups = new LinkedHashMap<>();

    // ----- Plugin SDK -----
    private final Map<String, Plugin> plugins = new LinkedHashMap<>();

    // ----- API SDK -----
    private final Map<String, ApiKey> apiKeys = new LinkedHashMap<>();
    private final Map<String, Webhook> webhooks = new LinkedHashMap<>();
    private final List<String> webhookLog = new ArrayList<>();

    // ----- Marketplace -----
    private final Map<String, MarketListing> market = new LinkedHashMap<>();
    private final Set<String> purchases = new LinkedHashSet<>();

    // ----- Offline Mode -----
    private boolean offlineMode;
    private final List<String> offlineQueue = new ArrayList<>();
    private final List<String> syncLog = new ArrayList<>();
    private long lastSyncAt;

    // ----- Cloud Sync -----
    private boolean cloudEnabled;
    private final Map<String, Device> devices = new LinkedHashMap<>();
    private int cloudConflicts;
    private long lastCloudSync;

    // ----- Security Operations Center -----
    private final Map<String, SocAlert> socAlerts = new LinkedHashMap<>();
    private final Map<String, Incident> incidents = new LinkedHashMap<>();
    private final List<String> timeline = new ArrayList<>();

    // ----- Quantum Cryptography -----
    private final Set<String> quantumLearned = new LinkedHashSet<>();

    // ----- Blockchain Security -----
    private final Map<String, Wallet> wallets = new LinkedHashMap<>();
    private final Map<String, Block> chain = new LinkedHashMap<>();
    private final Map<String, Contract> contracts = new LinkedHashMap<>();

    // ----- Machine Learning Security -----
    private final List<String> mlLog = new ArrayList<>();
    private int mlFlagged;

    // ----- Digital Forensics Workbench -----
    private final Map<String, ForensicCase> forensicCases = new LinkedHashMap<>();

    // ----- Global Events -----
    private final Map<String, GlobalEvent> globalEvents = new LinkedHashMap<>();
    private final Set<String> eventJoined = new LinkedHashSet<>();
    private final Map<String, Integer> eventScores = new LinkedHashMap<>();

    // ----- Enterprise Reports -----
    private final List<String> reportLog = new ArrayList<>();

    // ----- Future-Ready Architecture -----
    private final Map<String, ArchComponent> architecture = new LinkedHashMap<>();

    private int teamCounter;
    private int courseCounter;
    private int deptCounter;
    private int examCounter;
    private int labCounter;
    private int ctfCounter;
    private int notebookCounter;
    private int portfolioCounter;
    private int boardCounter;
    private int writeupCounter;
    private int pluginCounter;
    private int apiKeyCounter;
    private int webhookCounter;
    private int marketCounter;
    private int deviceCounter;
    private int socAlertCounter;
    private int incidentCounter;
    private int walletCounter;
    private int contractCounter;
    private int eventCounter;
    private int caseCounter;

    public EliteService(AcademyService academy) {
        this.academy = academy;
        this.operatorId = academy.getOperatorId();
    }

    // ================================================================
    // PERSISTENCE
    // ================================================================

    public void load() {
        try {
            File f = new File(FILE);
            if (!f.exists()) return;
            apply(new JSONObject(Files.readString(f.toPath())));
        } catch (Exception e) {
            System.err.println("[ELITE] Profile load failed (safe ignore): " + e.getMessage());
        }
    }

    private void apply(JSONObject j) {
        if (!operatorId.equals(j.optString("operatorId", operatorId))) return;
        JSONObject pp = j.optJSONObject("pathProgress");
        if (pp != null) for (String k : pp.keySet()) {
            JSONObject o = pp.getJSONObject(k);
            pathProgress.put(k, new PathProgress(k,
                o.optInt("lessons"), o.optInt("labs"), o.optInt("ctfs"), o.optInt("projects"),
                o.optInt("exam", -1)));
        }
        readSet(j, "pathCompleted", pathCompleted);
        readIntMap(j, "rangeStepsDone", rangeStepsDone);
        readSet(j, "rangeDone", rangeDone);
        readSet(j, "missionsDone", missionDone);
        readIntMap(j, "missionProgress", missionObjectiveProgress);
        JSONObject mr = j.optJSONObject("missionReports");
        if (mr != null) for (String k : mr.keySet()) missionReports.put(k, mr.optString(k));
        readSet(j, "storyDone", storyDone);
        readIntMap(j, "storyProgress", storySceneProgress);
        JSONObject ts = j.optJSONObject("teams");
        if (ts != null) for (String k : ts.keySet()) {
            JSONObject o = ts.getJSONObject(k);
            Team t = new Team(k, o.optString("name"), o.optString("motto"), o.optLong("created"));
            JSONObject m = o.optJSONObject("members");
            if (m != null) for (String mn : m.keySet()) t.members.put(mn, m.optInt(mn));
            JSONArray ch = o.optJSONArray("chat");
            if (ch != null) for (int i = 0; i < ch.length(); i++) t.chat.add(ch.getString(i));
            t.challengeTitle = o.optString("challengeTitle", "");
            t.challengeDescr = o.optString("challengeDescr", "");
            t.challengeAnswer = o.optString("challengeAnswer", "");
            t.challengeXp = o.optInt("challengeXp");
            t.challengeSolved = o.optBoolean("challengeSolved");
            t.certIssued = o.optBoolean("certIssued");
            teams.put(k, t);
        }
        JSONObject cs = j.optJSONObject("courses");
        if (cs != null) for (String k : cs.keySet()) {
            JSONObject o = cs.getJSONObject(k);
            Course c = new Course(k, o.optString("title"), o.optString("descr"), o.optLong("created"));
            JSONArray st = o.optJSONArray("students");
            if (st != null) for (int i = 0; i < st.length(); i++) c.students.add(st.getString(i));
            JSONArray as = o.optJSONArray("assignments");
            if (as != null) for (int i = 0; i < as.length(); i++) c.assignments.add(as.getString(i));
            JSONArray ce = o.optJSONArray("certs");
            if (ce != null) for (int i = 0; i < ce.length(); i++) c.certs.add(ce.getString(i));
            courses.put(k, c);
        }
        JSONObject ss = j.optJSONObject("students");
        if (ss != null) for (String k : ss.keySet()) {
            JSONObject o = ss.getJSONObject(k);
            students.put(k, new StudentRecord(k, o.optInt("xp"), o.optInt("days")));
        }
        JSONObject ds = j.optJSONObject("departments");
        if (ds != null) for (String k : ds.keySet()) {
            JSONObject o = ds.getJSONObject(k);
            Department d = new Department(k, o.optString("name"));
            JSONArray cl = o.optJSONArray("classes");
            if (cl != null) for (int i = 0; i < cl.length(); i++) d.classNames.add(cl.getString(i));
            JSONObject cstu = o.optJSONObject("classStudents");
            if (cstu != null) for (String cn : cstu.keySet()) {
                JSONArray names = cstu.getJSONArray(cn);
                List<String> list = new ArrayList<>();
                for (int i = 0; i < names.length(); i++) list.add(names.getString(i));
                d.classStudents.put(cn, list);
            }
            JSONArray exs = o.optJSONArray("exams");
            if (exs != null) for (int i = 0; i < exs.length(); i++) {
                JSONObject ex = exs.getJSONObject(i);
                UniExam ue = new UniExam(ex.optString("id"), k, ex.optString("className"),
                    ex.optString("title"), ex.optInt("marks"));
                JSONObject sc = ex.optJSONObject("scores");
                if (sc != null) for (String sn : sc.keySet()) ue.scores.put(sn, sc.optInt(sn));
                d.exams.add(ue);
            }
            JSONArray as = o.optJSONArray("assignments");
            if (as != null) for (int i = 0; i < as.length(); i++) d.assignments.add(as.getString(i));
            departments.put(k, d);
        }
        JSONObject er = j.optJSONObject("examResults");
        if (er != null) for (String k : er.keySet()) {
            JSONObject o = er.getJSONObject(k);
            examResults.put(k, new ExamResult(k, o.optInt("score"), o.optBoolean("pass"),
                o.optLong("ts"), o.optBoolean("integrity")));
        }
        JSONObject co = j.optJSONObject("companies");
        if (co != null) for (String k : co.keySet()) {
            JSONObject o = co.getJSONObject(k);
            Company c = new Company(k, o.optString("name"));
            JSONArray em = o.optJSONArray("employees");
            if (em != null) for (int i = 0; i < em.length(); i++) {
                JSONObject eo = em.getJSONObject(i);
                Employee e = new Employee(eo.optString("name"), eo.optString("role"));
                JSONObject mo = eo.optJSONObject("modules");
                if (mo != null) for (String mn : mo.keySet()) e.modules.put(mn, mo.optBoolean(mn));
                JSONObject cfo = eo.optJSONObject("compliance");
                if (cfo != null) for (String cn : cfo.keySet()) e.compliance.put(cn, cfo.optBoolean(cn));
                c.employees.add(e);
            }
            JSONArray rp = o.optJSONArray("reports");
            if (rp != null) for (int i = 0; i < rp.length(); i++) c.reports.add(rp.getString(i));
            companies.put(k, c);
        }
        JSONArray labs = j.optJSONArray("customLabs");
        if (labs != null) for (int i = 0; i < labs.length(); i++) {
            JSONObject o = labs.getJSONObject(i);
            CustomLab lab = new CustomLab(o.optString("id"), o.optString("type"), o.optString("title"),
                o.optString("prompt"), o.optString("answer"), o.optInt("xp"), o.optString("diff"));
            lab.solved = o.optBoolean("solved");
            customLabs.put(lab.id, lab);
        }
        JSONArray ctfs = j.optJSONArray("customCtfs");
        if (ctfs != null) for (int i = 0; i < ctfs.length(); i++) {
            JSONObject o = ctfs.getJSONObject(i);
            CustomCtf c = new CustomCtf(o.optString("id"), o.optString("title"), o.optString("category"),
                o.optString("difficulty"), o.optString("description"), o.optString("solution"),
                o.optInt("xp"), o.optInt("timer"), o.optLong("created"));
            JSONArray fl = o.optJSONArray("flags");
            if (fl != null) for (int f = 0; f < fl.length(); f++) c.flags.add(fl.getString(f));
            JSONArray hn = o.optJSONArray("hints");
            if (hn != null) for (int f = 0; f < hn.length(); f++) c.hints.add(hn.getString(f));
            JSONArray at = o.optJSONArray("attachments");
            if (at != null) for (int f = 0; f < at.length(); f++) c.attachments.add(at.getString(f));
            c.solved = o.optBoolean("solved");
            c.solvedAt = o.optLong("solvedAt");
            customCtfs.put(c.id, c);
        }
        JSONObject tm = j.optJSONObject("topicMastery");
        if (tm != null) for (String k : tm.keySet()) topicMastery.put(k, tm.optDouble(k, 0));
        readIntMap(j, "topicAttempts", topicAttempts);
        readIntMap(j, "topicCorrect", topicCorrect);
        JSONArray notes = j.optJSONArray("notebook");
        if (notes != null) for (int i = 0; i < notes.length(); i++) {
            JSONObject o = notes.getJSONObject(i);
            NotebookEntry ne = new NotebookEntry(o.optString("id"), o.optString("title"),
                o.optString("kind"), o.optLong("created"));
            ne.body = o.optString("body");
            ne.tags = o.optString("tags", "");
            notebookEntries.put(ne.id, ne);
        }
        JSONArray pit = j.optJSONArray("portfolio");
        if (pit != null) for (int i = 0; i < pit.length(); i++) {
            JSONObject o = pit.getJSONObject(i);
            PortfolioItem pi = new PortfolioItem(o.optString("id"), o.optString("kind"),
                o.optString("title"), o.optLong("created"));
            pi.url = o.optString("url", "");
            pi.notes = o.optString("notes", "");
            portfolioItems.put(pi.id, pi);
        }
        githubUrl = j.optString("githubUrl", "");
        linkedinUrl = j.optString("linkedinUrl", "");
        trustPoints = j.optInt("trustPoints");
        reputationPoints = j.optInt("reputationPoints");
        communityPoints = j.optInt("communityPoints");
        mentorRating = j.optInt("mentorRating");
        contributorRating = j.optInt("contributorRating");
        JSONObject bds = j.optJSONObject("boards");
        if (bds != null) for (String k : bds.keySet()) {
            JSONObject o = bds.getJSONObject(k);
            Board b = new Board(k, o.optString("title"), o.optString("descr"), o.optLong("created"));
            JSONArray th = o.optJSONArray("threads");
            if (th != null) for (int i = 0; i < th.length(); i++) {
                JSONObject to = th.getJSONObject(i);
                Thread t = new Thread(to.optString("id"), to.optString("author"), to.optString("title"));
                t.body = to.optString("body");
                t.likes = to.optInt("likes");
                JSONArray cm = to.optJSONArray("comments");
                if (cm != null) for (int x = 0; x < cm.length(); x++) t.comments.add(cm.getString(x));
                b.threads.add(t);
            }
            boards.put(k, b);
        }
        JSONObject ws = j.optJSONObject("writeups");
        if (ws != null) for (String k : ws.keySet()) {
            JSONObject o = ws.getJSONObject(k);
            Writeup w = new Writeup(k, o.optString("title"), o.optString("author"), o.optLong("created"));
            w.body = o.optString("body");
            w.score = o.optInt("score");
            JSONArray rv = o.optJSONArray("reviews");
            if (rv != null) for (int i = 0; i < rv.length(); i++) w.reviews.add(rv.getString(i));
            JSONArray cm = o.optJSONArray("comments");
            if (cm != null) for (int i = 0; i < cm.length(); i++) w.comments.add(cm.getString(i));
            writeups.put(k, w);
        }
        JSONObject pl = j.optJSONObject("plugins");
        if (pl != null) for (String k : pl.keySet()) {
            JSONObject o = pl.getJSONObject(k);
            Plugin p = new Plugin(k, o.optString("name"), o.optString("author"), o.optString("version"),
                o.optString("kind"), o.optString("descr"), o.optLong("registeredAt"));
            JSONObject pr = o.optJSONObject("params");
            if (pr != null) for (String pk : pr.keySet()) p.params.put(pk, pr.optString(pk));
            p.enabled = o.optBoolean("enabled");
            plugins.put(k, p);
        }
        JSONObject aks = j.optJSONObject("apiKeys");
        if (aks != null) for (String k : aks.keySet()) {
            JSONObject o = aks.getJSONObject(k);
            ApiKey ak = new ApiKey(k, o.optString("label"), o.optString("key"), o.optLong("created"));
            ak.lastUsed = o.optLong("lastUsed");
            ak.revoked = o.optBoolean("revoked");
            apiKeys.put(k, ak);
        }
        JSONObject whs = j.optJSONObject("webhooks");
        if (whs != null) for (String k : whs.keySet()) {
            JSONObject o = whs.getJSONObject(k);
            webhooks.put(k, new Webhook(k, o.optString("url"), o.optString("event"), o.optLong("created")));
        }
        JSONArray wl = j.optJSONArray("webhookLog");
        if (wl != null) for (int i = 0; i < wl.length(); i++) webhookLog.add(wl.getString(i));
        JSONObject mk = j.optJSONObject("market");
        if (mk != null) for (String k : mk.keySet()) {
            JSONObject o = mk.getJSONObject(k);
            MarketListing l = new MarketListing(k, o.optString("kind"), o.optString("title"),
                o.optString("descr"), o.optInt("price"), o.optString("publisher"), o.optLong("created"));
            l.rating = o.optDouble("rating");
            l.reviewCount = o.optInt("reviewCount");
            l.ratingSum = o.optInt("ratingSum");
            l.purchasedCount = o.optInt("purchasedCount");
            market.put(k, l);
        }
        readSet(j, "purchases", purchases);
        offlineMode = j.optBoolean("offlineMode");
        readList(j, "offlineQueue", offlineQueue);
        readList(j, "syncLog", syncLog);
        lastSyncAt = j.optLong("lastSyncAt");
        cloudEnabled = j.optBoolean("cloudEnabled");
        JSONObject dvs = j.optJSONObject("devices");
        if (dvs != null) for (String k : dvs.keySet()) {
            JSONObject o = dvs.getJSONObject(k);
            Device d = new Device(k, o.optString("name"), o.optLong("created"));
            d.snapshot = o.optString("snapshot", "");
            d.hash = o.optString("hash", "");
            d.lastSync = o.optLong("lastSync");
            devices.put(k, d);
        }
        cloudConflicts = j.optInt("cloudConflicts");
        lastCloudSync = j.optLong("lastCloudSync");
        JSONObject sas = j.optJSONObject("socAlerts");
        if (sas != null) for (String k : sas.keySet()) {
            JSONObject o = sas.getJSONObject(k);
            SocAlert a = new SocAlert(k, o.optString("severity"), o.optString("source"),
                o.optString("message"), o.optLong("ts"));
            a.acknowledged = o.optBoolean("acknowledged");
            socAlerts.put(k, a);
        }
        JSONObject ins = j.optJSONObject("incidents");
        if (ins != null) for (String k : ins.keySet()) {
            JSONObject o = ins.getJSONObject(k);
            Incident in = new Incident(k, o.optString("title"), o.optString("severity"),
                o.optString("descr"), o.optLong("ts"), o.optString("status"));
            JSONArray rl = o.optJSONArray("responseLog");
            if (rl != null) for (int i = 0; i < rl.length(); i++) in.responseLog.add(rl.getString(i));
            incidents.put(k, in);
        }
        readList(j, "socTimeline", timeline);
        readSet(j, "quantumLearned", quantumLearned);
        JSONObject wts = j.optJSONObject("wallets");
        if (wts != null) for (String k : wts.keySet()) {
            JSONObject o = wts.getJSONObject(k);
            Wallet w = new Wallet(k, o.optString("name"), o.optString("address"), o.optInt("balance"));
            wallets.put(k, w);
        }
        JSONObject ch = j.optJSONObject("chain");
        if (ch != null) for (String k : ch.keySet()) {
            JSONObject o = ch.getJSONObject(k);
            Block b = new Block(o.optInt("index"), o.optString("prevHash"), o.optString("hash"),
                o.optString("data"), o.optLong("ts"), o.optInt("nonce"));
            chain.put(k, b);
        }
        JSONObject cs2 = j.optJSONObject("contracts");
        if (cs2 != null) for (String k : cs2.keySet()) {
            JSONObject o = cs2.getJSONObject(k);
            Contract c = new Contract(k, o.optString("name"), o.optString("code"));
            c.calls = o.optInt("calls");
            contracts.put(k, c);
        }
        readList(j, "mlLog", mlLog);
        mlFlagged = j.optInt("mlFlagged");
        JSONObject fcs = j.optJSONObject("forensicCases");
        if (fcs != null) for (String k : fcs.keySet()) {
            JSONObject o = fcs.getJSONObject(k);
            ForensicCase fc = new ForensicCase(k, o.optString("title"), o.optLong("created"));
            fc.status = o.optString("status", "OPEN");
            JSONArray ev = o.optJSONArray("evidence");
            if (ev != null) for (int i = 0; i < ev.length(); i++) fc.evidence.add(ev.getString(i));
            JSONArray tl = o.optJSONArray("timeline");
            if (tl != null) for (int i = 0; i < tl.length(); i++) fc.timeline.add(tl.getString(i));
            JSONArray cn = o.optJSONArray("chain");
            if (cn != null) for (int i = 0; i < cn.length(); i++) fc.chain.add(cn.getString(i));
            forensicCases.put(k, fc);
        }
        JSONObject ges = j.optJSONObject("globalEvents");
        if (ges != null) for (String k : ges.keySet()) {
            JSONObject o = ges.getJSONObject(k);
            GlobalEvent ge = new GlobalEvent(k, o.optString("title"), o.optString("kind"),
                o.optLong("startsAt"), o.optLong("endsAt"), o.optInt("xpReward"));
            globalEvents.put(k, ge);
        }
        readSet(j, "eventJoined", eventJoined);
        readIntMap(j, "eventScores", eventScores);
        readList(j, "reportLog", reportLog);
        JSONObject arc = j.optJSONObject("architecture");
        if (arc != null) for (String k : arc.keySet()) {
            JSONObject o = arc.getJSONObject(k);
            ArchComponent ac = new ArchComponent(k, o.optString("name"), o.optString("target"),
                o.optString("status"), o.optBoolean("interfaceBound"));
            architecture.put(k, ac);
        }
        teamCounter = j.optInt("teamCounter");
        courseCounter = j.optInt("courseCounter");
        deptCounter = j.optInt("deptCounter");
        examCounter = j.optInt("examCounter");
        labCounter = j.optInt("labCounter");
        ctfCounter = j.optInt("ctfCounter");
        notebookCounter = j.optInt("notebookCounter");
        portfolioCounter = j.optInt("portfolioCounter");
        boardCounter = j.optInt("boardCounter");
        writeupCounter = j.optInt("writeupCounter");
        pluginCounter = j.optInt("pluginCounter");
        apiKeyCounter = j.optInt("apiKeyCounter");
        webhookCounter = j.optInt("webhookCounter");
        marketCounter = j.optInt("marketCounter");
        deviceCounter = j.optInt("deviceCounter");
        socAlertCounter = j.optInt("socAlertCounter");
        incidentCounter = j.optInt("incidentCounter");
        walletCounter = j.optInt("walletCounter");
        contractCounter = j.optInt("contractCounter");
        eventCounter = j.optInt("eventCounter");
        caseCounter = j.optInt("caseCounter");
    }

    private JSONObject toJson() {
        JSONObject j = new JSONObject();
        j.put("operatorId", operatorId);
        JSONObject pp = new JSONObject();
        for (Map.Entry<String, PathProgress> e : pathProgress.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("lessons", e.getValue().lessonsDone);
            o.put("labs", e.getValue().labsDone);
            o.put("ctfs", e.getValue().ctfsDone);
            o.put("projects", e.getValue().projectsDone);
            o.put("exam", e.getValue().examScore);
            pp.put(e.getKey(), o);
        }
        j.put("pathProgress", pp);
        writeSet(j, "pathCompleted", pathCompleted);
        writeIntMap(j, "rangeStepsDone", rangeStepsDone);
        writeSet(j, "rangeDone", rangeDone);
        writeSet(j, "missionsDone", missionDone);
        writeIntMap(j, "missionProgress", missionObjectiveProgress);
        JSONObject mr = new JSONObject();
        for (Map.Entry<String, String> e : missionReports.entrySet()) mr.put(e.getKey(), e.getValue());
        j.put("missionReports", mr);
        writeSet(j, "storyDone", storyDone);
        writeIntMap(j, "storyProgress", storySceneProgress);
        JSONObject ts = new JSONObject();
        for (Map.Entry<String, Team> e : teams.entrySet()) {
            Team t = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", t.name); o.put("motto", t.motto); o.put("created", t.created);
            JSONObject m = new JSONObject();
            for (Map.Entry<String, Integer> me : t.members.entrySet()) m.put(me.getKey(), me.getValue());
            o.put("members", m);
            JSONArray ch = new JSONArray();
            for (String s : t.chat) ch.put(s);
            o.put("chat", ch);
            o.put("challengeTitle", t.challengeTitle);
            o.put("challengeDescr", t.challengeDescr);
            o.put("challengeAnswer", t.challengeAnswer);
            o.put("challengeXp", t.challengeXp);
            o.put("challengeSolved", t.challengeSolved);
            o.put("certIssued", t.certIssued);
            ts.put(e.getKey(), o);
        }
        j.put("teams", ts);
        JSONObject cs = new JSONObject();
        for (Map.Entry<String, Course> e : courses.entrySet()) {
            Course c = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", c.title); o.put("descr", c.descr); o.put("created", c.created);
            o.put("students", new JSONArray(c.students));
            o.put("assignments", new JSONArray(c.assignments));
            o.put("certs", new JSONArray(c.certs));
            cs.put(e.getKey(), o);
        }
        j.put("courses", cs);
        JSONObject ss = new JSONObject();
        for (Map.Entry<String, StudentRecord> e : students.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("xp", e.getValue().xp);
            o.put("days", e.getValue().activeDays);
            ss.put(e.getKey(), o);
        }
        j.put("students", ss);
        JSONObject ds = new JSONObject();
        for (Map.Entry<String, Department> e : departments.entrySet()) {
            Department d = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", d.name);
            o.put("classes", new JSONArray(d.classNames));
            JSONObject cstu = new JSONObject();
            for (Map.Entry<String, List<String>> ce : d.classStudents.entrySet()) {
                cstu.put(ce.getKey(), new JSONArray(ce.getValue()));
            }
            o.put("classStudents", cstu);
            JSONArray exs = new JSONArray();
            for (UniExam ue : d.exams) {
                JSONObject ex = new JSONObject();
                ex.put("id", ue.id); ex.put("className", ue.className); ex.put("title", ue.title);
                ex.put("marks", ue.marks);
                JSONObject sc = new JSONObject();
                for (Map.Entry<String, Integer> se : ue.scores.entrySet()) sc.put(se.getKey(), se.getValue());
                ex.put("scores", sc);
                exs.put(ex);
            }
            o.put("exams", exs);
            o.put("assignments", new JSONArray(d.assignments));
            ds.put(e.getKey(), o);
        }
        j.put("departments", ds);
        JSONObject er = new JSONObject();
        for (Map.Entry<String, ExamResult> e : examResults.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("score", e.getValue().score);
            o.put("pass", e.getValue().pass);
            o.put("ts", e.getValue().ts);
            o.put("integrity", e.getValue().integrityOk);
            er.put(e.getKey(), o);
        }
        j.put("examResults", er);
        JSONObject co = new JSONObject();
        for (Map.Entry<String, Company> e : companies.entrySet()) {
            Company c = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", c.name);
            JSONArray em = new JSONArray();
            for (Employee emp : c.employees) {
                JSONObject eo = new JSONObject();
                eo.put("name", emp.name); eo.put("role", emp.role);
                JSONObject mo = new JSONObject();
                for (Map.Entry<String, Boolean> me : emp.modules.entrySet()) mo.put(me.getKey(), me.getValue());
                eo.put("modules", mo);
                JSONObject cfo = new JSONObject();
                for (Map.Entry<String, Boolean> ce2 : emp.compliance.entrySet()) cfo.put(ce2.getKey(), ce2.getValue());
                eo.put("compliance", cfo);
                em.put(eo);
            }
            o.put("employees", em);
            o.put("reports", new JSONArray(c.reports));
            co.put(e.getKey(), o);
        }
        j.put("companies", co);
        JSONArray labs = new JSONArray();
        for (CustomLab lab : customLabs.values()) {
            JSONObject o = new JSONObject();
            o.put("id", lab.id); o.put("type", lab.type); o.put("title", lab.title);
            o.put("prompt", lab.prompt); o.put("answer", lab.answer); o.put("xp", lab.xp);
            o.put("diff", lab.diff); o.put("solved", lab.solved);
            labs.put(o);
        }
        j.put("customLabs", labs);
        JSONArray ctfs = new JSONArray();
        for (CustomCtf c : customCtfs.values()) {
            JSONObject o = new JSONObject();
            o.put("id", c.id); o.put("title", c.title); o.put("category", c.category);
            o.put("difficulty", c.difficulty); o.put("description", c.description);
            o.put("solution", c.solution); o.put("xp", c.xp); o.put("timer", c.timerSec);
            o.put("created", c.created); o.put("flags", new JSONArray(c.flags));
            o.put("hints", new JSONArray(c.hints)); o.put("attachments", new JSONArray(c.attachments));
            o.put("solved", c.solved); o.put("solvedAt", c.solvedAt);
            ctfs.put(o);
        }
        j.put("customCtfs", ctfs);
        JSONObject tm = new JSONObject();
        for (Map.Entry<String, Double> e : topicMastery.entrySet()) tm.put(e.getKey(), e.getValue());
        j.put("topicMastery", tm);
        writeIntMap(j, "topicAttempts", topicAttempts);
        writeIntMap(j, "topicCorrect", topicCorrect);
        JSONArray notes = new JSONArray();
        for (NotebookEntry ne : notebookEntries.values()) {
            JSONObject o = new JSONObject();
            o.put("id", ne.id); o.put("title", ne.title); o.put("kind", ne.kind);
            o.put("body", ne.body); o.put("tags", ne.tags); o.put("created", ne.created);
            notes.put(o);
        }
        j.put("notebook", notes);
        JSONArray pit = new JSONArray();
        for (PortfolioItem pi : portfolioItems.values()) {
            JSONObject o = new JSONObject();
            o.put("id", pi.id); o.put("kind", pi.kind); o.put("title", pi.title);
            o.put("url", pi.url); o.put("notes", pi.notes); o.put("created", pi.created);
            pit.put(o);
        }
        j.put("portfolio", pit);
        j.put("githubUrl", githubUrl);
        j.put("linkedinUrl", linkedinUrl);
        j.put("trustPoints", trustPoints);
        j.put("reputationPoints", reputationPoints);
        j.put("communityPoints", communityPoints);
        j.put("mentorRating", mentorRating);
        j.put("contributorRating", contributorRating);
        JSONObject bds = new JSONObject();
        for (Map.Entry<String, Board> e : boards.entrySet()) {
            Board b = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", b.title); o.put("descr", b.descr); o.put("created", b.created);
            JSONArray th = new JSONArray();
            for (Thread t : b.threads) {
                JSONObject to = new JSONObject();
                to.put("id", t.id); to.put("author", t.author); to.put("title", t.title);
                to.put("body", t.body); to.put("likes", t.likes);
                to.put("comments", new JSONArray(t.comments));
                th.put(to);
            }
            o.put("threads", th);
            bds.put(e.getKey(), o);
        }
        j.put("boards", bds);
        JSONObject ws = new JSONObject();
        for (Map.Entry<String, Writeup> e : writeups.entrySet()) {
            Writeup w = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", w.title); o.put("author", w.author); o.put("body", w.body);
            o.put("score", w.score); o.put("created", w.created);
            o.put("reviews", new JSONArray(w.reviews));
            o.put("comments", new JSONArray(w.comments));
            ws.put(e.getKey(), o);
        }
        j.put("writeups", ws);
        JSONObject pl = new JSONObject();
        for (Map.Entry<String, Plugin> e : plugins.entrySet()) {
            Plugin p = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", p.name); o.put("author", p.author); o.put("version", p.version);
            o.put("kind", p.kind); o.put("descr", p.descr); o.put("registeredAt", p.registeredAt);
            o.put("enabled", p.enabled);
            JSONObject pr = new JSONObject();
            for (Map.Entry<String, String> pe : p.params.entrySet()) pr.put(pe.getKey(), pe.getValue());
            o.put("params", pr);
            pl.put(e.getKey(), o);
        }
        j.put("plugins", pl);
        JSONObject aks = new JSONObject();
        for (Map.Entry<String, ApiKey> e : apiKeys.entrySet()) {
            ApiKey ak = e.getValue();
            JSONObject o = new JSONObject();
            o.put("label", ak.label); o.put("key", ak.key); o.put("created", ak.created);
            o.put("lastUsed", ak.lastUsed); o.put("revoked", ak.revoked);
            aks.put(e.getKey(), o);
        }
        j.put("apiKeys", aks);
        JSONObject whs = new JSONObject();
        for (Map.Entry<String, Webhook> e : webhooks.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("url", e.getValue().url); o.put("event", e.getValue().event);
            o.put("created", e.getValue().created);
            whs.put(e.getKey(), o);
        }
        j.put("webhooks", whs);
        writeList(j, "webhookLog", webhookLog);
        JSONObject mk = new JSONObject();
        for (Map.Entry<String, MarketListing> e : market.entrySet()) {
            MarketListing l = e.getValue();
            JSONObject o = new JSONObject();
            o.put("kind", l.kind); o.put("title", l.title); o.put("descr", l.descr);
            o.put("price", l.price); o.put("publisher", l.publisher); o.put("created", l.created);
            o.put("rating", l.rating); o.put("reviewCount", l.reviewCount);
            o.put("ratingSum", l.ratingSum); o.put("purchasedCount", l.purchasedCount);
            mk.put(e.getKey(), o);
        }
        j.put("market", mk);
        writeSet(j, "purchases", purchases);
        j.put("offlineMode", offlineMode);
        writeList(j, "offlineQueue", offlineQueue);
        writeList(j, "syncLog", syncLog);
        j.put("lastSyncAt", lastSyncAt);
        j.put("cloudEnabled", cloudEnabled);
        JSONObject dvs = new JSONObject();
        for (Map.Entry<String, Device> e : devices.entrySet()) {
            Device d = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", d.name); o.put("created", d.created);
            o.put("snapshot", d.snapshot); o.put("hash", d.hash); o.put("lastSync", d.lastSync);
            dvs.put(e.getKey(), o);
        }
        j.put("devices", dvs);
        j.put("cloudConflicts", cloudConflicts);
        j.put("lastCloudSync", lastCloudSync);
        JSONObject sas = new JSONObject();
        for (Map.Entry<String, SocAlert> e : socAlerts.entrySet()) {
            SocAlert a = e.getValue();
            JSONObject o = new JSONObject();
            o.put("severity", a.severity); o.put("source", a.source); o.put("message", a.message);
            o.put("ts", a.ts); o.put("acknowledged", a.acknowledged);
            sas.put(e.getKey(), o);
        }
        j.put("socAlerts", sas);
        JSONObject ins = new JSONObject();
        for (Map.Entry<String, Incident> e : incidents.entrySet()) {
            Incident in = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", in.title); o.put("severity", in.severity); o.put("descr", in.descr);
            o.put("ts", in.ts); o.put("status", in.status);
            o.put("responseLog", new JSONArray(in.responseLog));
            ins.put(e.getKey(), o);
        }
        j.put("incidents", ins);
        writeList(j, "socTimeline", timeline);
        writeSet(j, "quantumLearned", quantumLearned);
        JSONObject wts = new JSONObject();
        for (Map.Entry<String, Wallet> e : wallets.entrySet()) {
            Wallet w = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", w.name); o.put("address", w.address); o.put("balance", w.balance);
            wts.put(e.getKey(), o);
        }
        j.put("wallets", wts);
        JSONObject ch = new JSONObject();
        for (Map.Entry<String, Block> e : chain.entrySet()) {
            Block b = e.getValue();
            JSONObject o = new JSONObject();
            o.put("index", b.index); o.put("prevHash", b.prevHash); o.put("hash", b.hash);
            o.put("data", b.data); o.put("ts", b.ts); o.put("nonce", b.nonce);
            ch.put(e.getKey(), o);
        }
        j.put("chain", ch);
        JSONObject cs2 = new JSONObject();
        for (Map.Entry<String, Contract> e : contracts.entrySet()) {
            JSONObject o = new JSONObject();
            o.put("name", e.getValue().name); o.put("code", e.getValue().code);
            o.put("calls", e.getValue().calls);
            cs2.put(e.getKey(), o);
        }
        j.put("contracts", cs2);
        writeList(j, "mlLog", mlLog);
        j.put("mlFlagged", mlFlagged);
        JSONObject fcs = new JSONObject();
        for (Map.Entry<String, ForensicCase> e : forensicCases.entrySet()) {
            ForensicCase fc = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", fc.title); o.put("created", fc.created); o.put("status", fc.status);
            o.put("evidence", new JSONArray(fc.evidence));
            o.put("timeline", new JSONArray(fc.timeline));
            o.put("chain", new JSONArray(fc.chain));
            fcs.put(e.getKey(), o);
        }
        j.put("forensicCases", fcs);
        JSONObject ges = new JSONObject();
        for (Map.Entry<String, GlobalEvent> e : globalEvents.entrySet()) {
            GlobalEvent ge = e.getValue();
            JSONObject o = new JSONObject();
            o.put("title", ge.title); o.put("kind", ge.kind); o.put("startsAt", ge.startsAt);
            o.put("endsAt", ge.endsAt); o.put("xpReward", ge.xpReward);
            ges.put(e.getKey(), o);
        }
        j.put("globalEvents", ges);
        writeSet(j, "eventJoined", eventJoined);
        writeIntMap(j, "eventScores", eventScores);
        writeList(j, "reportLog", reportLog);
        JSONObject arc = new JSONObject();
        for (Map.Entry<String, ArchComponent> e : architecture.entrySet()) {
            ArchComponent ac = e.getValue();
            JSONObject o = new JSONObject();
            o.put("name", ac.name); o.put("target", ac.target); o.put("status", ac.status);
            o.put("interfaceBound", ac.interfaceBound);
            arc.put(e.getKey(), o);
        }
        j.put("architecture", arc);
        j.put("teamCounter", teamCounter);
        j.put("courseCounter", courseCounter);
        j.put("deptCounter", deptCounter);
        j.put("examCounter", examCounter);
        j.put("labCounter", labCounter);
        j.put("ctfCounter", ctfCounter);
        j.put("notebookCounter", notebookCounter);
        j.put("portfolioCounter", portfolioCounter);
        j.put("boardCounter", boardCounter);
        j.put("writeupCounter", writeupCounter);
        j.put("pluginCounter", pluginCounter);
        j.put("apiKeyCounter", apiKeyCounter);
        j.put("webhookCounter", webhookCounter);
        j.put("marketCounter", marketCounter);
        j.put("deviceCounter", deviceCounter);
        j.put("socAlertCounter", socAlertCounter);
        j.put("incidentCounter", incidentCounter);
        j.put("walletCounter", walletCounter);
        j.put("contractCounter", contractCounter);
        j.put("eventCounter", eventCounter);
        j.put("caseCounter", caseCounter);
        return j;
    }

    public void save() {
        try {
            File dir = new File(APP_DIR);
            if (!dir.exists()) dir.mkdirs();
            Files.writeString(new File(FILE).toPath(), toJson().toString(2));
        } catch (Exception e) {
            System.err.println("[ELITE] Profile save failed (safe ignore): " + e.getMessage());
        }
    }

    private static void readSet(JSONObject j, String key, Set<String> out) {
        JSONArray a = j.optJSONArray(key);
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
    }

    private static void writeSet(JSONObject j, String key, Set<String> in) {
        JSONArray a = new JSONArray();
        for (String s : in) a.put(s);
        j.put(key, a);
    }

    private static void readIntMap(JSONObject j, String key, Map<String, Integer> out) {
        JSONObject o = j.optJSONObject(key);
        if (o != null) for (String k : o.keySet()) out.put(k, o.optInt(k));
    }

    private static void writeIntMap(JSONObject j, String key, Map<String, Integer> in) {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Integer> e : in.entrySet()) o.put(e.getKey(), e.getValue());
        j.put(key, o);
    }

    private static void readList(JSONObject j, String key, List<String> out) {
        JSONArray a = j.optJSONArray(key);
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
    }

    private static void writeList(JSONObject j, String key, List<String> in) {
        JSONArray a = new JSONArray();
        for (String s : in) a.put(s);
        j.put(key, a);
    }

    private void notify(String type, String title, String detail) {
        academy.notify(type, title, detail);
    }

    // ================================================================
    // 1. LEARNING PATH ENGINE
    // ================================================================

    /** Builds the catalog of the ten learning paths (deterministic, static). */
    public List<LearningPath> paths() {
        List<LearningPath> out = new ArrayList<>();
        out.add(learnPath("cryptofound", "Cryptography Foundations", "\uD83D\uDD10", "BEGINNER", 6,
            "Classical to modern ciphers: Caesar, XOR, AES and friends.", "Cryptography Foundations",
            new String[]{},
            lessons("Caesar Shift Anatomy", "Frequency Analysis Basics", "XOR Bitwise Operators", "Modern Symmetric Ciphers", "Hashing and Integrity"),
            labs("Cipher Wheel Lab", "XOR Drill", "Substitution Playground"),
            ctfs("Roman Walls", "Bit Flipper", "Hash Runner"),
            projects("Build a Caesar tool", "Mini XOR utility"),
            5));
        out.add(learnPath("appliedcrypto", "Applied Cryptography", "\uD83D\uDD12", "INTERMEDIATE", 10,
            "Real-world key exchange, PKI and hybrid schemes.", "Applied Cryptography",
            new String[]{"cryptofound"},
            lessons("Public Key Cryptography", "RSA Mathematics", "Key Exchange Protocols", "Digital Signatures", "PKI and Certificates"),
            labs("RSA Keygen Lab", "Diffie-Hellman Sim", "Signature Verifier"),
            ctfs("Prime Hunt", "Key Swap", "Signed Blob"),
            projects("End-to-end encrypted chat", "Certificate chain verifier"),
            5));
        out.add(learnPath("websec", "Web Security", "\uD83C\uDF10", "INTERMEDIATE", 12,
            "OWASP top ten, injection, XSS and session attacks.", "Web Security",
            new String[]{},
            lessons("HTTP Request Anatomy", "SQL Injection", "Cross-Site Scripting", "CSRF and Sessions", "Secure Headers"),
            labs("Injection Range", "XSS Range", "Session Hijack Sim"),
            ctfs("Query Breaker", "Script Weaver", "Token Snatcher"),
            projects("Secure login scaffold", "CSP hardening"),
            5));
        out.add(learnPath("blueteam", "Blue Team", "\uD83D\uDEE1\uFE0F", "INTERMEDIATE", 14,
            "Defensive operations, monitoring and response.", "Blue Team",
            new String[]{},
            lessons("Network Defense", "Log Analysis", "IDS/IPS Concepts", "Incident Response Lifecycle", "Threat Hunting Basics"),
            labs("SIEM Triage", "Firewall Rules Lab", "Log Parser"),
            ctfs("Alert Buried", "Rule Smith", "Forensic First Responder"),
            projects("Incident runbook", "Detection ruleset"),
            5));
        out.add(learnPath("redteam", "Red Team", "\uD83D\uDD25", "ADVANCED", 16,
            "Adversary simulation, exploitation and post-exploitation.", "Red Team",
            new String[]{"websec"},
            lessons("Reconnaissance", "Exploitation Fundamentals", "Privilege Escalation", "Persistence Techniques", "Lateral Movement"),
            labs("Recon Range", "Buffer Overflow Lab", "Post-Exploit Playground"),
            ctfs("Zero-Day Hunt", "Kernel Pwn", "Lateral Run"),
            projects("Adversary emulation plan", "Custom implant PoC"),
            5));
        out.add(learnPath("socanalyst", "SOC Analyst", "\uD83D\uDEA8", "INTERMEDIATE", 12,
            "Tier-1 monitoring, triage and escalation skills.", "SOC Analyst",
            new String[]{"blueteam"},
            lessons("SOC Operations", "Alert Triage", "Ticket Handling", "Network Traffic Analysis", "Email Security"),
            labs("Alert Triage Lab", "PCAP Reading Lab", "Phishing Sifter"),
            ctfs("The 3AM Alert", "Packet Scent", "Phish Finder"),
            projects("SOC playbook", "Triage checklist"),
            5));
        out.add(learnPath("forensics", "Digital Forensics", "\uD83D\uDD0E", "ADVANCED", 14,
            "Disk, memory and network evidence recovery.", "Digital Forensics",
            new String[]{},
            lessons("Evidence Handling", "Disk Imaging", "Memory Analysis", "Network Forensics", "Reporting Evidence"),
            labs("Disk Image Lab", "Memory Dump Lab", "Metadata Miner"),
            ctfs("Deleted But Not Gone", "RAM Raider", "Exif Hunter"),
            projects("Forensic report template", "Case study: exfil"),
            5));
        out.add(learnPath("malware", "Malware Analysis", "\uD83E\uDDE0", "ADVANCED", 18,
            "Static and dynamic analysis of malicious samples.", "Malware Analysis",
            new String[]{"forensics"},
            lessons("Malware Taxonomy", "Static Analysis", "Dynamic Analysis", "Packing and Obfuscation", "C2 Communication"),
            labs("Strings Lab", "Sandbox Run", "Decoder Ring"),
            ctfs("Packed Beast", "C2 Chatter", "Obfuscation Maze"),
            projects("Analysis report on a dropper", "YARA ruleset"),
            5));
        out.add(learnPath("binary", "Binary Exploitation", "\uD83D\uDC7E", "EXPERT", 20,
            "Memory corruption and exploit development.", "Binary Exploitation",
            new String[]{"redteam"},
            lessons("Assembly Primer", "The Stack", "Buffer Overflows", "ROP Chains", "Shellcode Crafting"),
            labs("Stack Smash Lab", "Heap Lab", "ROP Lab"),
            ctfs("Ret2Win", "Shellcode Runner", "Format Fury"),
            projects("Exploit a patched binary", "Custom ROP chain"),
            5));
        out.add(learnPath("reversing", "Reverse Engineering", "\u2699\uFE0F", "EXPERT", 22,
            "Decompiling binaries and defeating protections.", "Reverse Engineering",
            new String[]{"binary"},
            lessons("Binary Formats", "Disassembly", "Decompilation", "Anti-Reverse Tricks", "Protocol Reverse"),
            labs("Disassembler Lab", "CrackMe Lab", "Protocol Reverse Lab"),
            ctfs("Serial Validator", "License Check", "Custom Protocol"),
            projects("Unpack a protected binary", "Recreate a proprietary format"),
            5));
        return out;
    }

    private static LearningPath learnPath(String id, String title, String icon, String level, int hours,
                                          String descr, String certTitle, String[] prereqs,
                                          String[] lessons, String[] labs, String[] ctfs, String[] projects,
                                          int examQuestions) {
        return new LearningPath(id, title, icon, level, hours, descr, certTitle,
            List.of(prereqs), List.of(lessons), List.of(labs), List.of(ctfs), List.of(projects), examQuestions);
    }

    private static String[] lessons(String... a) { return a; }
    private static String[] labs(String... a) { return a; }
    private static String[] ctfs(String... a) { return a; }
    private static String[] projects(String... a) { return a; }

    public LearningPath path(String id) {
        for (LearningPath p : paths()) if (p.id.equals(id)) return p;
        return null;
    }

    public List<String> prerequisites(String pathId) {
        LearningPath p = path(pathId);
        return p == null ? List.of() : p.prereqs;
    }

    /** True when every prerequisite path is completed. */
    public boolean canStart(String pathId) {
        LearningPath p = path(pathId);
        if (p == null) return false;
        for (String pre : p.prereqs) if (!pathCompleted.contains(pre)) return false;
        return true;
    }

    public PathProgress progress(String pathId) {
        PathProgress pr = pathProgress.get(pathId);
        if (pr == null) pr = new PathProgress(pathId, 0, 0, 0, 0, -1);
        return pr;
    }

    public boolean isPathCompleted(String pathId) { return pathCompleted.contains(pathId); }

    public void completeLesson(String pathId, int idx) {
        LearningPath p = path(pathId);
        if (p == null || !canStart(pathId)) return;
        PathProgress pr = progress(pathId);
        if (idx >= 0 && idx < p.lessons.size() && idx >= pr.lessonsDone) {
            pr.lessonsDone = idx + 1;
            pathProgress.put(pathId, pr);
            awardPathStep(pathId, "Lesson", 15);
        }
    }

    public void completeLab(String pathId, int idx) {
        LearningPath p = path(pathId);
        if (p == null || !canStart(pathId)) return;
        PathProgress pr = progress(pathId);
        if (idx >= 0 && idx < p.labs.size() && idx >= pr.labsDone) {
            pr.labsDone = idx + 1;
            pathProgress.put(pathId, pr);
            awardPathStep(pathId, "Lab", 30);
        }
    }

    public void completeCtf(String pathId, int idx) {
        LearningPath p = path(pathId);
        if (p == null || !canStart(pathId)) return;
        PathProgress pr = progress(pathId);
        if (idx >= 0 && idx < p.ctfs.size() && idx >= pr.ctfsDone) {
            pr.ctfsDone = idx + 1;
            pathProgress.put(pathId, pr);
            awardPathStep(pathId, "CTF", 60);
        }
    }

    public void completeProject(String pathId, int idx) {
        LearningPath p = path(pathId);
        if (p == null || !canStart(pathId)) return;
        PathProgress pr = progress(pathId);
        if (idx >= 0 && idx < p.projects.size() && idx >= pr.projectsDone) {
            pr.projectsDone = idx + 1;
            pathProgress.put(pathId, pr);
            awardPathStep(pathId, "Project", 100);
        }
    }

    private void awardPathStep(String pathId, String kind, int xp) {
        academy.awardXp(xp);
        notify("XP", "PATH PROGRESS", kind + " complete in \"" + path(pathId).title + "\" \u2014 +" + xp + " XP.");
    }

    /** Grades the path final exam. Pass mark 70%. On pass the path certificate is unlocked. */
    public int takePathExam(String pathId, List<Integer> answers) {
        LearningPath p = path(pathId);
        if (p == null || !canStart(pathId)) return -1;
        PathProgress pr = progress(pathId);
        List<ExamQuestion> qs = pathExamQuestions(pathId);
        int correct = 0;
        for (int i = 0; i < qs.size() && i < answers.size(); i++) {
            if (answers.get(i) != null && answers.get(i).equals(qs.get(i).answerIdx)) correct++;
        }
        int score = (int) Math.round(correct * 100.0 / Math.max(1, qs.size()));
        if (score >= pr.examScore) pr.examScore = score;
        pathProgress.put(pathId, pr);
        if (score >= 70 && !pathCompleted.contains(pathId)) {
            pathCompleted.add(pathId);
            academy.addCertPoints(150);
            academy.addCoins(80);
            notify("ACHIEVEMENT", "PATH COMPLETED",
                "Certificate unlocked: " + p.certTitle + " (final score " + score + "%).");
        } else if (score < 70) {
            notify("SYSTEM", "EXAM FAILED", p.title + " final exam scored " + score + "% \u2014 70% required.");
        }
        save();
        return score;
    }

    /** Deterministic 5-question exam for a path; grading re-derives the same questions. */
    public List<ExamQuestion> pathExamQuestions(String pathId) {
        Random r = new Random(("pathexam|" + pathId).hashCode());
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < QUESTION_BANK.length; i++) indices.add(i);
        java.util.Collections.shuffle(indices, r);
        List<ExamQuestion> qs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String[] q = QUESTION_BANK[indices.get(i)];
            String[] options = {q[1], q[2], q[3], q[4]};
            int correct = Integer.parseInt(q[5]);
            String[] shuffled = new String[4];
            int[] order = {0, 1, 2, 3};
            for (int j = 0; j < 4; j++) {
                int k = j + r.nextInt(4 - j);
                int tmp = order[j]; order[j] = order[k]; order[k] = tmp;
            }
            for (int j = 0; j < 4; j++) shuffled[j] = options[order[j]];
            int newCorrect = 0;
            for (int j = 0; j < 4; j++) if (order[j] == correct) newCorrect = j;
            qs.add(new ExamQuestion(q[0], shuffled, newCorrect));
        }
        return qs;
    }

    // ================================================================
    // 2. CYBER RANGE
    // ================================================================

    public List<RangeEnv> ranges() {
        List<RangeEnv> out = new ArrayList<>();
        out.add(range("bank", "Secure Bank", "\uD83C\uDFE6", "HIGH",
            "A bank detected suspicious encrypted traffic on its SWIFT gateway.",
            step("Analyze the suspicious traffic", "Which cipher family does the captured XOR traffic use?", "xor", 40),
            step("Recover the encryption key", "What is the recovered key word?", "VAULT", 60),
            step("Decrypt the exfiltrated files", "Submit the decrypted flag.", "UC{BANKBREACH}", 80),
            step("Identify the attacker", "Which actor name is tied to the intrusion?", "ghostnet", 100)));
        out.add(range("corp", "Corporate Network", "\uD83C\uDFE2", "MEDIUM",
            "A marketing laptop on the corporate network is beaconing to an external IP.",
            step("Trace the beacon traffic", "Which port is the beacon using?", "8443", 40),
            step("Decode the C2 payload", "What is the decoded C2 instruction?", "EXFIL", 60),
            step("Locate the infected host", "Which hostname is the source?", "MARVIN", 80),
            step("Contain the outbreak", "Which rule name blocks the beacon?", "beacon-block", 100)));
        out.add(range("hospital", "Hospital", "\uD83C\uDFE5", "HIGH",
            "Ransomware has encrypted patient records on the ICU file server.",
            step("Identify the ransomware family", "Which ransomware family name is it?", "lockscreen", 40),
            step("Recover the encryption key", "What key decrypts the samples?", "ICU-KEY", 60),
            step("Restore the patient records", "Submit the recovery flag.", "UC{PATIENTSAFE}", 80),
            step("Patch the entry vector", "Which CVE is the suspected entry vector?", "CVE-2021-34527", 100)));
        out.add(range("govt", "Government Agency", "\uD83C\uDFDB\uFE0F", "CRITICAL",
            "A state-sponsored actor is probing the ministry network.",
            step("Analyze the spear-phish", "Which document type carried the dropper?", "RTF", 50),
            step("Extract the implant", "What is the implant's registered name?", "stealthkit", 70),
            step("Map the data exfil", "Which department shared the most bytes?", "finance", 90),
            step("Disrupt the C2 channel", "Which domain is the C2 endpoint?", "updates-service.net", 110),
            step("Submit the full report", "Submit the containment flag.", "UC{COUNTERINTEL}", 130)));
        out.add(range("cloud", "Cloud Infrastructure", "\u2601\uFE0F", "MEDIUM",
            "An S3 bucket has been left publicly writable and mined for coin.",
            step("Audit the misconfiguration", "Which policy error exposed the bucket?", "public-write", 40),
            step("Decrypt the coin miner payload", "What is the miner's pool address flag?", "UC{MINED}", 60),
            step("Rotate the leaked keys", "Which key prefix leaked to the bucket?", "AKIA", 80),
            step("Lock down the account", "Which feature blocks public access?", "block-public-access", 100)));
        out.add(range("iot", "IoT Smart Home", "\uD83C\uDFE1", "LOW",
            "A smart speaker is participating in a large DDoS botnet.",
            step("Identify the weak firmware", "Which default credential opened the device?", "admin/admin", 30),
            step("Decode the botnet traffic", "What is the command word?", "MIRAI", 50),
            step("Segment the home network", "Which VLAN rule isolates the devices?", "iot-vlan", 70)));
        out.add(range("ics", "Industrial Control System", "\uD83C\uDFED\uFE0F", "CRITICAL",
            "An attacker is rewriting ladder logic in a power plant PLC.",
            step("Detect the protocol abuse", "Which protocol was hijacked?", "modbus", 60),
            step("Decrypt the attacker beacon", "What is the beacon payload?", "SYS-OFF", 80),
            step("Freeze the rogue logic", "Which tag was overwritten?", "TANK-VALVE", 100),
            step("Trace the initial foothold", "Which engineering station was the origin?", "ENG-03", 110),
            step("Restore safe operations", "Submit the safety flag.", "UC{GRIDSTABLE}", 130)));
        return out;
    }

    private static RangeEnv range(String id, String name, String icon, String threat, String descr, RangeStep... steps) {
        return new RangeEnv(id, name, icon, threat, descr, List.of(steps));
    }

    private static RangeStep step(String prompt, String question, String answer, int xp) {
        return new RangeStep(prompt, question, answer, xp);
    }

    public RangeEnv range(String id) {
        for (RangeEnv r : ranges()) if (r.id.equals(id)) return r;
        return null;
    }

    public int rangeStepsDone(String id) { return rangeStepsDone.getOrDefault(id, 0); }

    public boolean isRangeDone(String id) { return rangeDone.contains(id); }

    public double rangePercent(String id) {
        RangeEnv r = range(id);
        if (r == null) return 0;
        return rangeStepsDone.getOrDefault(id, 0) * 100.0 / Math.max(1, r.steps.size());
    }

    /** Attempts a range step answer. Solving every step clears the range. */
    public boolean solveRangeStep(String rangeId, int idx, String answer) {
        RangeEnv r = range(rangeId);
        if (r == null || idx < 0 || idx >= r.steps.size()) return false;
        if (rangeStepsDone.getOrDefault(rangeId, 0) > idx) return true;
        RangeStep s = r.steps.get(idx);
        if (answer == null || !answer.trim().equalsIgnoreCase(s.answer)) return false;
        rangeStepsDone.put(rangeId, Math.max(rangeStepsDone.getOrDefault(rangeId, 0), idx + 1));
        academy.awardXp(s.xp);
        if (rangeStepsDone.get(rangeId) >= r.steps.size() && !rangeDone.contains(rangeId)) {
            rangeDone.add(rangeId);
            academy.addCoins(60);
            notify("ACHIEVEMENT", "RANGE CLEARED", r.name + " \u2014 all incidents resolved.");
        }
        save();
        return true;
    }

    // ================================================================
    // 3. MISSION MODE
    // ================================================================

    public List<EliteMission> missions() {
        List<EliteMission> out = new ArrayList<>();
        out.add(mission("ledger", "Operation Ledger Breach", "\uD83C\uDFE6",
            "A bank has detected suspicious encrypted traffic between two internal nodes.",
            500, 150, "Mission: Ledger Breach",
            obj("traffic", "Analyze the encrypted traffic", "Which cipher family is in play?", "xor", 40),
            obj("key", "Recover the encryption key", "What is the recovered key?", "HACK", 60),
            obj("decrypt", "Decrypt the stolen files", "Submit the decrypted flag.", "UC{VICTORY}", 80),
            obj("attacker", "Identify the attacker", "Who is the suspected actor?", "ghostnet", 100)));
        out.add(mission("phish", "Phishing Harvest", "\uD83C\uDFD7\uFE0F",
            "A spear-phishing wave is harvesting credentials across the org.",
            400, 120, "Mission: Phishing Harvest",
            obj("email", "Triage the malicious email", "Which sender domain sent the lure?", "secure-login.co", 40),
            obj("link", "Decode the phishing link", "Where does the encoded link point?", "login-clone.net", 60),
            obj("payload", "Extract the credential stealer", "What is the payload name?", "keylogger", 80),
            obj("remediate", "Notify the affected users", "Submit the takedown flag.", "UC{PHISHSTOPPED}", 100)));
        out.add(mission("ransom", "Ransomware Incident", "\uD83C\uDFE5",
            "Hospital file servers are being encrypted in waves by ransomware.",
            600, 180, "Mission: Ransomware Incident",
            obj("sample", "Analyze the ransomware sample", "Which family is it?", "lockscreen", 50),
            obj("key", "Recover the decryption key", "What key unlocks the files?", "ICU-KEY", 80),
            obj("restore", "Restore critical systems", "Submit the restore flag.", "UC{RECOVERED}", 100),
            obj("contain", "Block the propagation", "Which SMB share was the vector?", "FILE-SRV", 120)));
        out.add(mission("insider", "Insider Threat", "\uD83C\uDFDB\uFE0F",
            "An employee is exfiltrating customer data via covert channels.",
            550, 160, "Mission: Insider Threat",
            obj("channel", "Find the covert channel", "Which protocol hides the exfil?", "steganography", 50),
            obj("image", "Decode the hidden payload", "Submit the hidden flag.", "UC{WHISTLEBLOW}", 80),
            obj("actor", "Identify the insider", "Which employee badge matches?", "E-4471", 100),
            obj("report", "Contain and preserve evidence", "Submit the containment flag.", "UC{SEALED}", 110)));
        out.add(mission("supply", "Supply Chain Compromise", "\u2601\uFE0F",
            "A vendor library update has been weaponized and deployed internally.",
            650, 200, "Mission: Supply Chain Compromise",
            obj("manifest", "Audit the build manifest", "Which package was poisoned?", "logger-utils", 60),
            obj("backdoor", "Extract the backdoor", "What is the implant's beacon?", "cdn-payload.dev", 90),
            obj("trace", "Trace affected deployments", "Which zone auto-deployed it?", "eu-west-2", 110),
            obj("vet", "Harden the pipeline", "Submit the hardened flag.", "UC{CHAINSAFE}", 130)));
        return out;
    }

    private static EliteMission mission(String id, String title, String icon, String brief,
                                        int xpReward, int coins, String badge, MissionObjective... objs) {
        return new EliteMission(id, title, icon, brief, List.of(objs), xpReward, coins, badge);
    }

    private static MissionObjective obj(String id, String text, String question, String answer, int xp) {
        return new MissionObjective(id, text, question, answer, xp);
    }

    public EliteMission mission(String id) {
        for (EliteMission m : missions()) if (m.id.equals(id)) return m;
        return null;
    }

    public boolean isMissionDone(String id) { return missionDone.contains(id); }

    public int missionObjectivesDone(String id) { return missionObjectiveProgress.getOrDefault(id, 0); }

    public double missionPercent(String id) {
        EliteMission m = mission(id);
        if (m == null) return 0;
        return missionObjectiveProgress.getOrDefault(id, 0) * 100.0 / Math.max(1, m.objectives.size());
    }

    public String missionReport(String id) { return missionReports.getOrDefault(id, ""); }

    public void submitMissionReport(String id, String text) {
        if (mission(id) == null) return;
        missionReports.put(id, text == null ? "" : text);
        save();
    }

    /** Attempts one mission objective answer. When all objectives are done the mission is complete. */
    public boolean solveMissionObjective(String id, int idx, String answer) {
        EliteMission m = mission(id);
        if (m == null || missionDone.contains(id) || idx < 0 || idx >= m.objectives.size()) return false;
        if (missionObjectiveProgress.getOrDefault(id, 0) > idx) return true;
        MissionObjective o = m.objectives.get(idx);
        if (answer == null || !answer.trim().equalsIgnoreCase(o.answer)) return false;
        missionObjectiveProgress.put(id, Math.max(missionObjectiveProgress.getOrDefault(id, 0), idx + 1));
        academy.awardXp(o.xp);
        if (missionObjectiveProgress.get(id) >= m.objectives.size()) {
            missionDone.add(id);
            academy.addCoins(m.coins);
            academy.addCertPoints(40);
            notify("ACHIEVEMENT", "MISSION COMPLETE", m.title + " \u2014 " + m.badge + " unlocked.");
        }
        save();
        return true;
    }

    // ================================================================
    // 4. STORY MODE
    // ================================================================

    public List<StoryEpisode> episodes() {
        List<StoryEpisode> out = new ArrayList<>();
        out.add(story("ep1", "Operation Black Cipher", "\uD83D\uDD75\uFE0F", 1,
            "The grid goes dark. A rogue cipher is encrypting the city's financial records.",
            scene("The Blackout", "The exchange's logs show one cipher repeated.", "Which cipher family keeps appearing?", "caesar", 50),
            scene("The First Key", "A shift value is hidden in the mailbox.", "What shift value unlocks the logs?", "13", 60),
            scene("The Dead Drop", "An encrypted message points to a warehouse.", "Submit the decoded dead-drop flag.", "UC{WAREHOUSE}", 80)));
        out.add(story("ep2", "Ghost Network", "\uD83D\uDC7B", 2,
            "The encrypted records funnel into a ghost network of anonymous relays.",
            scene("Relay Trace", "One relay still answers with plaintext.", "Which relay answered?", "relay-9", 60),
            scene("The Handshake", "A XOR handshake is embedded in the chatter.", "What is the handshake word?", "ECHO", 70),
            scene("Ghost Call", "The controller books a final call.", "Submit the ghost-call flag.", "UC{GHOSTNET}", 90)));
        out.add(story("ep3", "Shadow Encryption", "\uD83C\uDF19", 3,
            "The controller upgrades to a shadow encryption scheme no analyst has seen.",
            scene("New Scheme", "Traffic is base64-encoded and shifted.", "Which cipher family is layered first?", "base64", 70),
            scene("Crack the Layer", "The layered cipher is XOR with a single byte.", "What is the single-byte key?", "0x7F", 90),
            scene("Shadow Files", "Encrypted documents hold the network map.", "Submit the shadow flag.", "UC{SHADOW}", 110)));
        out.add(story("ep4", "Quantum Threat", "\uD83E\uDDEC", 4,
            "A quantum-ready key distribution scheme is subverted by the adversary.",
            scene("Qubit Leak", "Photons carry a leaking key schedule.", "Which scheme leaked?", "bb84", 90),
            scene("Recover the Schedule", "The key schedule is encrypted with AES.", "What is the recovered key block?", "QUDEC-KEY", 110),
            scene("Break the Oracle", "The oracle accepts one magic input.", "Submit the quantum flag.", "UC{QUANTUM}", 130)));
        out.add(story("ep5", "Nation State Attack", "\uD83C\uDFF0\uFE0F", 5,
            "A nation state activates its final cascade. Everything comes together.",
            scene("Final Foothold", "The implant names itself in traffic.", "What is the implant name?", "nationkit", 110),
            scene("Cascade", "Multiple campaigns converge on one server.", "Which server ID is ground zero?", "SRV-0", 130),
            scene("Counterstrike", "Submit the final campaign flag.", "Submit the nation-state flag.", "UC{PATRIOT}", 150)));
        return out;
    }

    private static StoryEpisode story(String id, String title, String icon, int part, String summary, StoryScene... scenes) {
        return new StoryEpisode(id, title, icon, part, summary, List.of(scenes));
    }

    private static StoryScene scene(String title, String prompt, String question, String answer, int xp) {
        return new StoryScene(title, prompt, question, answer, xp);
    }

    public StoryEpisode episode(String id) {
        for (StoryEpisode e : episodes()) if (e.id.equals(id)) return e;
        return null;
    }

    public boolean isEpisodeUnlocked(String id) {
        int part = episode(id).part;
        if (part <= 1) return true;
        for (StoryEpisode e : episodes()) if (e.part == part - 1 && storyDone.contains(e.id)) return true;
        return false;
    }

    public boolean isEpisodeDone(String id) { return storyDone.contains(id); }

    public int storyScenesDone(String id) { return storySceneProgress.getOrDefault(id, 0); }

    public double storyPercent(String id) {
        StoryEpisode e = episode(id);
        if (e == null) return 0;
        return storySceneProgress.getOrDefault(id, 0) * 100.0 / Math.max(1, e.scenes.size());
    }

    public boolean solveStoryScene(String epId, int idx, String answer) {
        StoryEpisode e = episode(epId);
        if (e == null || !isEpisodeUnlocked(epId) || storyDone.contains(epId) || idx < 0 || idx >= e.scenes.size()) return false;
        if (storySceneProgress.getOrDefault(epId, 0) > idx) return true;
        StoryScene s = e.scenes.get(idx);
        if (answer == null || !answer.trim().equalsIgnoreCase(s.answer)) return false;
        storySceneProgress.put(epId, Math.max(storySceneProgress.getOrDefault(epId, 0), idx + 1));
        academy.awardXp(s.xp);
        if (storySceneProgress.get(epId) >= e.scenes.size()) {
            storyDone.add(epId);
            academy.addCoins(50);
            academy.addCertPoints(30);
            notify("ACHIEVEMENT", "EPISODE COMPLETE", e.title + " \u2014 next episode unlocked.");
        }
        save();
        return true;
    }

    // ================================================================
    // 5. TEAM MODE
    // ================================================================

    public Team createTeam(String name, String motto) {
        if (name == null || name.trim().isEmpty()) return null;
        teamCounter++;
        String id = "team_" + teamCounter;
        Team t = new Team(id, name.trim(), motto == null ? "" : motto, System.currentTimeMillis());
        t.members.put(operatorId, academy.getTotalXp());
        teams.put(id, t);
        save();
        return t;
    }

    public List<Team> getTeams() { return new ArrayList<>(teams.values()); }

    public Team team(String id) { return teams.get(id); }

    public Team inviteMember(String teamId, String name) {
        Team t = teams.get(teamId);
        if (t == null || name == null || name.trim().isEmpty()) return t;
        if (t.members.containsKey(name.trim())) return t;
        Random r = new Random(("member|" + teamId + "|" + name).hashCode());
        t.members.put(name.trim(), 40 + r.nextInt(1400));
        t.chat.add("[SYSTEM] " + name.trim() + " joined " + t.name + ".");
        save();
        return t;
    }

    public Team teamPost(String teamId, String message) {
        Team t = teams.get(teamId);
        if (t == null || message == null || message.trim().isEmpty()) return t;
        t.chat.add(operatorId + ": " + message.trim());
        if (t.chat.size() > 60) t.chat.subList(0, t.chat.size() - 60).clear();
        save();
        return t;
    }

    public Team challengeTeam(String teamId) {
        Team t = teams.get(teamId);
        if (t == null || t.challengeSolved) return t;
        Challenge c = academy.generateChallenge("MEDIUM");
        t.challengeTitle = c.title;
        t.challengeDescr = c.descr + " Hint: " + c.hint;
        t.challengeAnswer = c.flag;
        t.challengeXp = c.xp;
        save();
        return t;
    }

    public boolean solveTeamChallenge(String teamId, String answer) {
        Team t = teams.get(teamId);
        if (t == null || t.challengeSolved || t.challengeAnswer.isEmpty()) return false;
        if (answer == null || !answer.trim().equalsIgnoreCase(t.challengeAnswer)) return false;
        t.challengeSolved = true;
        int bonus = t.challengeXp;
        for (Map.Entry<String, Integer> m : t.members.entrySet()) m.setValue(m.getValue() + 20);
        academy.awardXp(bonus);
        t.members.put(operatorId, academy.getTotalXp());
        if (t.sharedXp() >= 1000 && !t.certIssued) {
            t.certIssued = true;
            academy.addCertPoints(120);
            notify("ACHIEVEMENT", "TEAM CERTIFICATE", t.name + " earned its team certificate (shared XP " + t.sharedXp() + ").");
        }
        notify("XP", "TEAM CHALLENGE", t.name + " solved \"" + t.challengeTitle + "\" \u2014 +" + bonus + " XP shared.");
        save();
        return true;
    }

    // ================================================================
    // 6. MENTOR MODE
    // ================================================================

    public Course createCourse(String title, String descr, List<String> studentNames) {
        if (title == null || title.trim().isEmpty()) return null;
        courseCounter++;
        Course c = new Course("course_" + courseCounter, title.trim(), descr == null ? "" : descr, System.currentTimeMillis());
        if (studentNames != null) {
            for (String s : studentNames) {
                if (s != null && !s.trim().isEmpty()) {
                    String n = s.trim();
                    c.students.add(n);
                    students.putIfAbsent(n, new StudentRecord(n, 0, 0));
                }
            }
        }
        courses.put(c.id, c);
        save();
        return c;
    }

    public List<Course> getCourses() { return new ArrayList<>(courses.values()); }

    public Course getCourse(String courseId) { return courses.get(courseId); }

    public Course course(String id) { return courses.get(id); }

    public Course assignChallenge(String courseId, String title, String descr, String answer, int xp) {
        Course c = courses.get(courseId);
        if (c == null) return c;
        String line = "[" + title + "] " + descr + "  \u2022  XP " + xp;
        c.assignments.add(line);
        for (String s : c.students) {
            StudentRecord r = students.get(s);
            if (r != null) r.xp += 10;
        }
        save();
        return c;
    }

    public Course recordStudentProgress(String courseId, String student, int xpDelta) {
        Course c = courses.get(courseId);
        if (c == null) return c;
        StudentRecord r = students.get(student);
        if (r == null) {
            r = new StudentRecord(student, 0, 0);
            students.put(student, r);
        }
        r.xp += xpDelta;
        if (!c.students.contains(student)) c.students.add(student);
        save();
        return c;
    }

    public Course issueCertificate(String courseId, String student) {
        Course c = courses.get(courseId);
        if (c == null) return c;
        String line = student + " \u2014 " + java.time.LocalDate.now() + " \u2014 " + c.title;
        c.certs.add(line);
        academy.addCertPoints(100);
        notify("ACHIEVEMENT", "MENTOR CERTIFICATE", student + " certified in " + c.title + ".");
        save();
        return c;
    }

    public List<StudentRecord> getStudents() { return new ArrayList<>(students.values()); }

    public String mentorReport(String courseId) {
        Course c = courses.get(courseId);
        if (c == null) return "No course found.";
        int assigned = c.assignments.size();
        int total = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("COURSE: ").append(c.title).append("\n");
        sb.append("STUDENTS (").append(c.students.size()).append("):\n");
        for (String s : c.students) {
            StudentRecord r = students.getOrDefault(s, new StudentRecord(s, 0, 0));
            int completion = assigned == 0 ? 100 : Math.min(100, r.xp / Math.max(1, assigned * 15) * 100);
            total += r.xp;
            sb.append("  \u2022 ").append(s).append(" \u2014 XP ").append(r.xp)
              .append(", activity ").append(r.activeDays).append("d, completion ").append(completion).append("%\n");
        }
        sb.append("CERTIFICATES ISSUED: ").append(c.certs.size()).append("\n");
        for (String cert : c.certs) sb.append("  \u2022 ").append(cert).append("\n");
        sb.append("TOTAL STUDENT XP: ").append(total).append("\n");
        return sb.toString();
    }

    // ================================================================
    // 7. UNIVERSITY MODE
    // ================================================================

    public Department createDepartment(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        deptCounter++;
        Department d = new Department("dept_" + deptCounter, name.trim());
        departments.put(d.id, d);
        save();
        return d;
    }

    public List<Department> getDepartments() { return new ArrayList<>(departments.values()); }

    public Department getDepartment(String deptId) { return departments.get(deptId); }

    public Department department(String id) { return departments.get(id); }

    public Department addClass(String deptId, String className) {
        Department d = departments.get(deptId);
        if (d == null || className == null || className.trim().isEmpty()) return d;
        if (!d.classNames.contains(className.trim())) d.classNames.add(className.trim());
        save();
        return d;
    }

    public Department addStudent(String deptId, String className, String student) {
        Department d = departments.get(deptId);
        if (d == null || className == null || student == null) return d;
        List<String> list = d.classStudents.computeIfAbsent(className.trim(), k -> new ArrayList<>());
        if (!list.contains(student.trim())) list.add(student.trim());
        save();
        return d;
    }

    public Department addAssignment(String deptId, String className, String title, String due, int marks) {
        Department d = departments.get(deptId);
        if (d == null) return d;
        d.assignments.add("[" + className + "] " + title + " \u2022 due " + due + " \u2022 " + marks + " marks");
        save();
        return d;
    }

    public UniExam createUniExam(String deptId, String className, String title, int marks) {
        Department d = departments.get(deptId);
        if (d == null) return null;
        examCounter++;
        UniExam e = new UniExam("exam_" + examCounter, deptId, className, title, marks);
        d.exams.add(e);
        save();
        return e;
    }

    public UniExam recordUniExamScore(String deptId, String examId, String student, int score) {
        Department d = departments.get(deptId);
        if (d == null) return null;
        for (UniExam e : d.exams) {
            if (e.id.equals(examId)) {
                e.scores.put(student, score);
                save();
                return e;
            }
        }
        return null;
    }

    public String semesterReport(String deptId, String semester) {
        Department d = departments.get(deptId);
        if (d == null) return "No department found.";
        int totalStudents = 0;
        double avg = 0;
        int examCount = d.exams.size();
        StringBuilder sb = new StringBuilder();
        sb.append("DEPARTMENT: ").append(d.name).append(" \u2014 ").append(semester).append("\n");
        for (UniExam e : d.exams) {
            sb.append("  EXAM: ").append(e.title).append(" (").append(e.className).append(", ").append(e.marks).append(" marks)\n");
            for (Map.Entry<String, Integer> sc : e.scores.entrySet()) {
                totalStudents++;
                avg += sc.getValue();
            }
        }
        avg = totalStudents == 0 ? 0 : avg / totalStudents;
        sb.append("CLASSES: ").append(d.classNames.size()).append("\n");
        sb.append("EXAMS HELD: ").append(examCount).append("\n");
        sb.append("ASSIGNMENTS: ").append(d.assignments.size()).append("\n");
        sb.append("AVERAGE EXAM SCORE: ").append(String.format("%.1f", avg)).append("\n");
        return sb.toString();
    }

    public List<String[]> departmentLeaderboard(String deptId) {
        Department d = departments.get(deptId);
        Map<String, List<Integer>> scores = new LinkedHashMap<>();
        if (d != null) {
            for (UniExam e : d.exams) {
                for (Map.Entry<String, Integer> sc : e.scores.entrySet()) {
                    scores.computeIfAbsent(sc.getKey(), k -> new ArrayList<>()).add(sc.getValue());
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : scores.entrySet()) {
            int sum = 0;
            for (int v : e.getValue()) sum += v;
            rows.add(new String[]{e.getKey(), e.getValue().size() + "", sum + ""});
        }
        rows.sort((a, b) -> Integer.compare(Integer.parseInt(b[2]), Integer.parseInt(a[2])));
        return rows;
    }

    // ================================================================
    // 8. COMPANY TRAINING MODE
    // ================================================================

    private static final String[] TRAINING_MODULES = {
        "Security Awareness", "Phishing Defense", "Password Hygiene", "Incident Reporting",
        "Data Classification", "Remote Work Security", "Compliance Essentials", "Safe Browsing"
    };

    private static final String[] COMPLIANCE_ITEMS = {
        "GDPR Training", "ISO 27001 Awareness", "HIPAA Privacy", "PCI-DSS Basics",
        "Acceptable Use Policy", "Bring Your Own Device"
    };

    public Company createCompany(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Company c = new Company("comp_" + (companies.size() + 1), name.trim());
        companies.put(c.id, c);
        save();
        return c;
    }

    public List<Company> getCompanies() { return new ArrayList<>(companies.values()); }

    public Company getCompany(String companyId) { return companies.get(companyId); }

    public Company company(String id) { return companies.get(id); }

    public String[] trainingModules() { return TRAINING_MODULES.clone(); }

    public String[] complianceItems() { return COMPLIANCE_ITEMS.clone(); }

    public Company addEmployee(String companyId, String name, String role) {
        Company c = companies.get(companyId);
        if (c == null || name == null || name.trim().isEmpty()) return c;
        for (Employee e : c.employees) if (e.name.equals(name.trim())) return c;
        Employee e = new Employee(name.trim(), role == null ? "Staff" : role.trim());
        for (String m : TRAINING_MODULES) e.modules.put(m, false);
        for (String ci : COMPLIANCE_ITEMS) e.compliance.put(ci, false);
        c.employees.add(e);
        save();
        return c;
    }

    public Company completeModule(String companyId, String employee, String module) {
        Company c = companies.get(companyId);
        if (c == null) return c;
        for (Employee e : c.employees) {
            if (e.name.equals(employee)) {
                e.modules.put(module, true);
                academy.awardXp(20);
                save();
                return c;
            }
        }
        return c;
    }

    public Company completeCompliance(String companyId, String employee, String item) {
        Company c = companies.get(companyId);
        if (c == null) return c;
        for (Employee e : c.employees) {
            if (e.name.equals(employee)) {
                e.compliance.put(item, true);
                save();
                return c;
            }
        }
        return c;
    }

    public String managerReport(String companyId) {
        Company c = companies.get(companyId);
        if (c == null) return "No company found.";
        StringBuilder sb = new StringBuilder();
        sb.append("COMPANY: ").append(c.name).append("\n");
        sb.append("EMPLOYEES: ").append(c.employees.size()).append("\n");
        for (Employee e : c.employees) {
            int done = 0;
            for (boolean b : e.modules.values()) if (b) done++;
            int compDone = 0;
            for (boolean b : e.compliance.values()) if (b) compDone++;
            int pct = e.modules.isEmpty() ? 0 : (int) Math.round(done * 100.0 / e.modules.size());
            sb.append("  \u2022 ").append(e.name).append(" (").append(e.role).append(") \u2014 ")
              .append(done).append("/").append(e.modules.size()).append(" modules, ")
              .append(compDone).append("/").append(e.compliance.size()).append(" compliance, ")
              .append(pct).append("% complete\n");
        }
        sb.append("REPORTS RUN: ").append(c.reports.size()).append("\n");
        return sb.toString();
    }

    /** Exports a CSV analytics dump for the company and writes it under ~/.ucsuite/analytics/. */
    public File exportAnalytics(String companyId) throws Exception {
        Company c = companies.get(companyId);
        if (c == null) return null;
        StringBuilder csv = new StringBuilder("employee,role,module,complete\n");
        for (Employee e : c.employees) {
            for (Map.Entry<String, Boolean> m : e.modules.entrySet()) {
                csv.append("\"").append(e.name).append("\",\"").append(e.role).append("\",\"")
                   .append(m.getKey()).append("\",").append(m.getValue() ? "yes" : "no").append("\n");
            }
        }
        File dir = new File(APP_DIR + File.separator + "analytics");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, sanitize(c.name) + "_analytics.csv");
        Files.writeString(out.toPath(), csv.toString());
        c.reports.add(java.time.LocalDate.now() + " \u2014 CSV exported");
        save();
        return out;
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    // ================================================================
    // 9. EXAM MODE
    // ================================================================

    private static final String[][] QUESTION_BANK = {
        {"Which cipher uses a repeating keyword to shift letters?", "Vigenere", "Caesar", "ROT13", "Base64", "0"},
        {"What does XOR stand for?", "Exclusive OR", "Exclusive Read", "Extreme Output", "X Outer", "0"},
        {"Which algorithm is a symmetric block cipher?", "AES", "RSA", "SHA-256", "Diffie-Hellman", "0"},
        {"Which function is one-way and used for integrity?", "SHA-256", "AES", "RSA", "DES", "0"},
        {"A Caesar cipher with shift 3 encodes 'HELLO' as:", "KHOOR", "EBIIL", "GIFMM", "MJQQT", "0"},
        {"Which family does NOT use a secret key?", "SHA-256 hashing", "AES", "Caesar", "XOR", "0"},
        {"What is the modulus component of an RSA key?", "n", "e", "d", "p", "0"},
        {"In base64, how many bits does each character encode?", "6", "8", "4", "16", "0"},
        {"Which of these is a stream cipher?", "RC4", "AES", "3DES", "DES", "0"},
        {"What is the name for cracking Vigenere by repeated letters?", "Kasiski examination", "Brute force", "Frequency shift", "Rainbow table", "0"},
        {"Which header mitigates clickjacking?", "X-Frame-Options", "Cache-Control", "ETag", "User-Agent", "0"},
        {"SQL injection targets which layer?", "Database", "Firewall", "DNS", "Router", "0"},
        {"What does a CSRF token protect against?", "Cross-site request forgery", "Buffer overflows", "Phishing links", "DDoS", "0"},
        {"Which protocol is used to encrypt web traffic?", "TLS", "FTP", "SMTP", "SNMP", "0"},
        {"A hash collision means:", "Two inputs share a digest", "The key was reused", "The cipher failed", "The salt leaked", "0"},
        {"Which is the correct port for HTTPS?", "443", "80", "22", "25", "0"},
        {"What is the decrypted result of ROT13('NOP')?", "ABC", "OPQ", "MNO", "CBA", "0"},
        {"Which data structure backs a blockchain?", "Linked blocks with hashes", "A binary tree", "A queue", "A stack", "0"},
        {"Which attack feeds a program malicious data to read memory?", "Buffer overflow", "Phishing", "SQL injection", "Session fixation", "0"},
        {"What is a zero-day?", "An unknown unpatched vulnerability", "A type of virus", "A firewall rule", "A hash type", "0"},
        {"Which tool is commonly used for network scanning?", "Nmap", "Notepad", "Photoshop", "Excel", "0"},
        {"In hex, what is decimal 255?", "FF", "10", "100", "FE", "0"},
        {"What does SOC stand for?", "Security Operations Center", "System Output Cache", "Secure Online Certificate", "Service Object Core", "0"},
        {"Which practice prevents password reuse damage?", "Unique passwords per site", "Long birthday", "Same key everywhere", "No password manager", "0"}
    };

    public Exam generateExam(int questionCount) {
        int count = Math.max(5, Math.min(questionCount, QUESTION_BANK.length));
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < QUESTION_BANK.length; i++) indices.add(i);
        java.util.Collections.shuffle(indices, rnd);
        examCounter++;
        List<ExamQuestion> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String[] q = QUESTION_BANK[indices.get(i)];
            String[] options = {q[1], q[2], q[3], q[4]};
            int correct = Integer.parseInt(q[5]);
            String[] shuffled = new String[4];
            int[] order = {0, 1, 2, 3};
            for (int j = 0; j < 4; j++) {
                int k = j + rnd.nextInt(4 - j);
                int tmp = order[j]; order[j] = order[k]; order[k] = tmp;
            }
        for (int j = 0; j < 4; j++) shuffled[j] = options[order[j]];
        int newCorrect = 0;
        for (int j = 0; j < 4; j++) if (order[j] == correct) newCorrect = j;
        questions.add(new ExamQuestion(q[0], shuffled, newCorrect));
    }
    currentExam = new Exam("exam_" + examCounter, questions, count * 60);
    return currentExam;
}

    /** Grades an exam submission; answers are the selected option index per question. */
    public ExamResult submitExam(String examId, List<Integer> answers, long startedAtMs) {
        if (examResults.containsKey(examId)) return examResults.get(examId);
        Exam e = currentExam;
        if (e == null || !e.id.equals(examId)) {
            ExamResult r = new ExamResult(examId, 0, false, System.currentTimeMillis(), false);
            examResults.put(examId, r);
            save();
            return r;
        }
        int correct = 0;
        for (int i = 0; i < e.questions.size() && i < answers.size(); i++) {
            if (answers.get(i) != null && answers.get(i).equals(e.questions.get(i).answerIdx)) correct++;
        }
        int score = (int) Math.round(correct * 100.0 / Math.max(1, e.questions.size()));
        long duration = System.currentTimeMillis() - startedAtMs;
        boolean integrity = duration >= e.durationSec * 1000L - 2000 && answers.size() == e.questions.size();
        ExamResult r = new ExamResult(examId, score, score >= 70, System.currentTimeMillis(), integrity);
        examResults.put(examId, r);
        if (r.pass) {
            academy.addCertPoints(120);
            academy.addCoins(60);
            notify("ACHIEVEMENT", "EXAM PASSED", "Score " + score + "% \u2014 certificate unlocked.");
        } else {
            notify("SYSTEM", "EXAM RESULT", "Score " + score + "% \u2014 70% required to pass.");
        }
        currentExam = null;
        save();
        return r;
    }

    public List<ExamResult> getExamResults() { return new ArrayList<>(examResults.values()); }

    public Exam getExam(String examId) { return currentExam != null && currentExam.id.equals(examId) ? currentExam : null; }

    // ================================================================
    // 10. LAB BUILDER
    // ================================================================

    public CustomLab createLab(String type, String title, String prompt, String answer, int xp, String diff) {
        if (title == null || title.trim().isEmpty() || answer == null || answer.trim().isEmpty()) return null;
        labCounter++;
        CustomLab lab = new CustomLab("lab_" + labCounter, type == null ? "CIPHER" : type,
            title.trim(), prompt == null ? "" : prompt, answer.trim(), Math.max(10, xp), diff == null ? "MEDIUM" : diff);
        customLabs.put(lab.id, lab);
        save();
        return lab;
    }

    public List<CustomLab> getCustomLabs() { return new ArrayList<>(customLabs.values()); }

    public CustomLab customLab(String id) { return customLabs.get(id); }

    public boolean solveCustomLab(String id, String answer) {
        CustomLab lab = customLabs.get(id);
        if (lab == null || lab.solved || answer == null || !answer.trim().equalsIgnoreCase(lab.answer)) return false;
        lab.solved = true;
        academy.awardXp(lab.xp);
        academy.addCoins(10);
        notify("ACHIEVEMENT", "LAB COMPLETE", "\"" + lab.title + "\" \u2014 +" + lab.xp + " XP.");
        save();
        return true;
    }

    public boolean deleteCustomLab(String id) {
        if (customLabs.remove(id) != null) { save(); return true; }
        return false;
    }

    // ================================================================
    // 11. CUSTOM CTF BUILDER
    // ================================================================

    private static final String[] CTF_CATEGORIES = {
        "Cryptography", "Web", "Forensics", "Reverse", "Binary", "OSINT", "Pwn", "Misc"
    };

    public List<String> ctfCategories() { return java.util.Arrays.asList(CTF_CATEGORIES); }

    public CustomCtf createCtf(String title, String category, String difficulty, String description,
                               String solution, int xp, int timerSec) {
        ctfCounter++;
        CustomCtf c = new CustomCtf("ctf_" + ctfCounter,
            title.trim(), category == null ? "Misc" : category,
            difficulty == null ? "MEDIUM" : difficulty,
            description == null ? "" : description,
            solution == null ? "" : solution, xp, Math.max(0, timerSec), System.currentTimeMillis());
        customCtfs.put(c.id, c);
        notify("ACHIEVEMENT", "CTF PUBLISHED", "\"" + c.title + "\" added to the challenge vault.");
        save();
        return c;
    }

    public List<CustomCtf> getCustomCtfs() { return new ArrayList<>(customCtfs.values()); }

    public CustomCtf getCustomCtf(String id) { return customCtfs.get(id); }

    public CustomCtf addCtfFlag(String ctfId, String flag) {
        CustomCtf c = customCtfs.get(ctfId);
        if (c == null) return null;
        if (flag != null && !flag.trim().isEmpty() && !c.flags.contains(flag.trim())) c.flags.add(flag.trim());
        save();
        return c;
    }

    public CustomCtf addCtfHint(String ctfId, String hint) {
        CustomCtf c = customCtfs.get(ctfId);
        if (c == null) return null;
        if (hint != null && !hint.trim().isEmpty()) c.hints.add(hint.trim());
        save();
        return c;
    }

    public CustomCtf addCtfAttachment(String ctfId, String attachment) {
        CustomCtf c = customCtfs.get(ctfId);
        if (c == null) return null;
        if (attachment != null && !attachment.trim().isEmpty()) c.attachments.add(attachment.trim());
        save();
        return c;
    }

    public boolean solveCtf(String ctfId, String flag) {
        CustomCtf c = customCtfs.get(ctfId);
        if (c == null || c.solved || flag == null) return false;
        boolean hit = false;
        for (String f : c.flags) {
            if (f.trim().equalsIgnoreCase(flag.trim())) { hit = true; break; }
        }
        if (!hit) return false;
        c.solved = true;
        c.solvedAt = System.currentTimeMillis();
        academy.awardXp(c.xp);
        academy.addCoins(20);
        addReputation(5);
        addCommunityPoints(10);
        notify("ACHIEVEMENT", "CTF SOLVED", "\"" + c.title + "\" cracked \u2014 +" + c.xp + " XP.");
        save();
        return true;
    }

    public boolean deleteCtf(String id) {
        if (customCtfs.remove(id) != null) { save(); return true; }
        return false;
    }

    // ================================================================
    // 12. AI CURRICULUM ENGINE
    // ================================================================

    private static final String[] CURRICULUM_TOPICS = {
        "XOR", "CAESAR", "VIGENERE", "AES", "RSA", "BASE64", "SHA256",
        "STEGANOGRAPHY", "FORENSICS", "NETWORKING", "REVERSING", "MALWARE"
    };

    public List<String> curriculumTopics() { return java.util.Arrays.asList(CURRICULUM_TOPICS); }

    public double mastery(String topic) {
        if (topicMastery.containsKey(topic)) return topicMastery.get(topic);
        return Math.max(0, Math.min(100, academy.proficiency(topic)));
    }

    public void recordAnswer(String topic, boolean correct) {
        String key = topic == null ? "GENERAL" : topic.toUpperCase();
        int att = topicAttempts.getOrDefault(key, 0) + 1;
        int cor = topicCorrect.getOrDefault(key, 0) + (correct ? 1 : 0);
        topicAttempts.put(key, att);
        topicCorrect.put(key, cor);
        topicMastery.put(key, Math.max(0.0, Math.min(100.0, Math.round(cor * 100.0 / att * 10) / 10.0)));
        save();
    }

    public List<String[]> detectWeakTopics() {
        List<String[]> out = new ArrayList<>();
        for (String t : CURRICULUM_TOPICS) {
            double m = mastery(t);
            if (m < 60.0) out.add(new String[]{t, String.valueOf((int) Math.round(m))});
        }
        out.sort((a, b) -> Integer.compare(Integer.parseInt(a[1]), Integer.parseInt(b[1])));
        return out;
    }

    private String lessonFor(String topic) {
        switch (topic) {
            case "XOR": return "XOR Bitwise Cryptography";
            case "CAESAR": return "Caesar & ROT13 Shifts";
            case "VIGENERE": return "Vigenere & Kasiski Analysis";
            case "AES": return "Advanced Encryption Standard";
            case "RSA": return "RSA & Modular Arithmetic";
            case "BASE64": return "Encoding Layers";
            case "SHA256": return "Hashing & Integrity";
            case "STEGANOGRAPHY": return "LSB Steganography";
            case "FORENSICS": return "Disk & Memory Forensics";
            case "NETWORKING": return "Traffic Analysis";
            case "REVERSING": return "Static Analysis";
            default: return "Malware Triage";
        }
    }

    public List<String> recommendLessons() {
        List<String> out = new ArrayList<>();
        for (String[] w : detectWeakTopics()) out.add("Lesson: " + lessonFor(w[0]));
        return out;
    }

    public List<String> recommendLabs() {
        List<String> out = new ArrayList<>();
        for (String[] w : detectWeakTopics()) out.add("Lab: " + lessonFor(w[0]) + " Lab");
        return out;
    }

    public List<String> recommendRevision() {
        List<String> out = new ArrayList<>();
        for (String[] w : detectWeakTopics()) {
            out.add("Revision: revisit " + lessonFor(w[0]) + " \u2014 mastery " + w[1] + "%");
        }
        return out;
    }

    public double averageMastery() {
        double sum = 0;
        for (String t : CURRICULUM_TOPICS) sum += mastery(t);
        return CURRICULUM_TOPICS.length == 0 ? 0 : sum / CURRICULUM_TOPICS.length;
    }

    public int predictCompletionDays() {
        int weak = detectWeakTopics().size();
        double avg = averageMastery();
        int base = Math.max(1, (int) Math.ceil((100.0 - avg) / 15.0));
        return Math.max(1, base + weak / 2);
    }

    public String curriculumReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("AI CURRICULUM ENGINE\n");
        sb.append("Average mastery: ").append((int) Math.round(averageMastery())).append("%\n");
        sb.append("Predicted completion: ").append(predictCompletionDays()).append(" days\n\n");
        sb.append("WEAK TOPICS (below 60%):\n");
        List<String[]> weak = detectWeakTopics();
        if (weak.isEmpty()) sb.append("  none \u2014 all topics mastered!\n");
        for (String[] w : weak) sb.append("  \u2022 ").append(w[0]).append(" \u2014 ").append(w[1]).append("%\n");
        sb.append("\nRECOMMENDED LESSONS:\n");
        for (String r : recommendLessons()) sb.append("  \u2022 ").append(r).append("\n");
        sb.append("\nRECOMMENDED LABS:\n");
        for (String r : recommendLabs()) sb.append("  \u2022 ").append(r).append("\n");
        sb.append("\nREVISION PLAN:\n");
        for (String r : recommendRevision()) sb.append("  \u2022 ").append(r).append("\n");
        return sb.toString();
    }

    // ================================================================
    // 13. DIGITAL NOTEBOOK
    // ================================================================

    public NotebookEntry createNote(String title, String kind, String body) {
        notebookCounter++;
        NotebookEntry ne = new NotebookEntry("note_" + notebookCounter,
            title == null ? "Untitled" : title, kind == null ? "NOTE" : kind, System.currentTimeMillis());
        ne.body = body == null ? "" : body;
        notebookEntries.put(ne.id, ne);
        save();
        return ne;
    }

    public List<NotebookEntry> getNotebookEntries() { return new ArrayList<>(notebookEntries.values()); }

    public List<NotebookEntry> getNotebookEntries(String kind) {
        List<NotebookEntry> out = new ArrayList<>();
        for (NotebookEntry ne : notebookEntries.values()) {
            if (ne.kind.equalsIgnoreCase(kind)) out.add(ne);
        }
        return out;
    }

    public NotebookEntry editNote(String id, String body) {
        NotebookEntry ne = notebookEntries.get(id);
        if (ne == null) return null;
        ne.body = body == null ? "" : body;
        save();
        return ne;
    }

    public boolean deleteNote(String id) {
        if (notebookEntries.remove(id) != null) { save(); return true; }
        return false;
    }

    public String notebookExport() {
        StringBuilder sb = new StringBuilder();
        sb.append("DIGITAL NOTEBOOK EXPORT\n");
        for (NotebookEntry ne : notebookEntries.values()) {
            sb.append("[").append(ne.kind).append("] ").append(ne.title).append("\n");
            sb.append("  ").append(ne.body.replace("\n", "\n  ")).append("\n");
        }
        return sb.toString();
    }

    // ================================================================
    // 14. PORTFOLIO
    // ================================================================

    public PortfolioItem addPortfolioItem(String kind, String title, String url, String notes) {
        portfolioCounter++;
        PortfolioItem pi = new PortfolioItem("pf_" + portfolioCounter,
            kind == null ? "PROJECT" : kind, title, System.currentTimeMillis());
        pi.url = url == null ? "" : url;
        pi.notes = notes == null ? "" : notes;
        portfolioItems.put(pi.id, pi);
        save();
        return pi;
    }

    public List<PortfolioItem> getPortfolioItems() { return new ArrayList<>(portfolioItems.values()); }

    public List<PortfolioItem> getPortfolioItems(String kind) {
        List<PortfolioItem> out = new ArrayList<>();
        for (PortfolioItem pi : portfolioItems.values()) {
            if (pi.kind.equalsIgnoreCase(kind)) out.add(pi);
        }
        return out;
    }

    public boolean deletePortfolioItem(String id) {
        if (portfolioItems.remove(id) != null) { save(); return true; }
        return false;
    }

    public void setGithubUrl(String url) { this.githubUrl = url == null ? "" : url; save(); }
    public String getGithubUrl() { return githubUrl; }
    public void setLinkedinUrl(String url) { this.linkedinUrl = url == null ? "" : url; save(); }
    public String getLinkedinUrl() { return linkedinUrl; }

    /** Pulls earned certificates, solved labs/CTFs and exam passes into the portfolio automatically. */
    public int autoCollectPortfolio() {
        int added = 0;
        for (String pid : pathCompleted) {
            LearningPath p = path(pid);
            if (p == null) continue;
            if (!portfolioHas("CERT", p.certTitle)) {
                addPortfolioItem("CERT", p.certTitle, "", "Certified on completion of the " + p.title + " path.");
                added++;
            }
        }
        for (CustomLab lab : customLabs.values()) {
            if (lab.solved && !portfolioHas("LAB", lab.title)) {
                addPortfolioItem("LAB", lab.title, "", "Solved " + lab.type + " lab worth " + lab.xp + " XP.");
                added++;
            }
        }
        for (CustomCtf c : customCtfs.values()) {
            if (c.solved && !portfolioHas("LAB", c.title)) {
                addPortfolioItem("LAB", c.title, "", "Cleared " + c.category + " CTF worth " + c.xp + " XP.");
                added++;
            }
        }
        for (ExamResult r : examResults.values()) {
            if (r.pass && !portfolioHas("ACHIEVEMENT", "Exam " + r.examId + " PASS")) {
                addPortfolioItem("ACHIEVEMENT", "Exam " + r.examId + " PASS", "", "Proctored exam scored " + r.score + "%.");
                added++;
            }
        }
        return added;
    }

    private boolean portfolioHas(String kind, String title) {
        for (PortfolioItem pi : portfolioItems.values()) {
            if (pi.kind.equalsIgnoreCase(kind) && pi.title.equalsIgnoreCase(title)) return true;
        }
        return false;
    }

    public String portfolioText() {
        StringBuilder sb = new StringBuilder();
        sb.append("PORTFOLIO\n");
        for (PortfolioItem pi : portfolioItems.values()) {
            sb.append("[").append(pi.kind).append("] ").append(pi.title);
            if (!pi.url.isEmpty()) sb.append(" \u2014 ").append(pi.url);
            sb.append("\n");
            if (!pi.notes.isEmpty()) sb.append("   ").append(pi.notes).append("\n");
        }
        if (!githubUrl.isEmpty()) sb.append("GitHub: ").append(githubUrl).append("\n");
        if (!linkedinUrl.isEmpty()) sb.append("LinkedIn: ").append(linkedinUrl).append("\n");
        return sb.toString();
    }

    // ================================================================
    // 15. RESUME GENERATOR
    // ================================================================

    public String generateResume() {
        StringBuilder sb = new StringBuilder();
        sb.append(LoginScreenUserName()).append(" \u2014 CRYPTOGRAPHIC OPERATOR\n");
        sb.append("==========================================\n");
        sb.append("Total XP ").append(academy.getTotalXp())
          .append(" \u2022 Trust ").append(trustPoints)
          .append(" \u2022 Reputation ").append(reputationPoints).append("\n\n");
        sb.append("SKILLS\n");
        List<String> skills = new ArrayList<>();
        for (String t : CURRICULUM_TOPICS) {
            if (mastery(t) >= 60.0) skills.add(t + " (" + (int) Math.round(mastery(t)) + "%)");
        }
        for (String pid : pathCompleted) {
            LearningPath p = path(pid);
            if (p != null) skills.add(p.title);
        }
        if (skills.isEmpty()) sb.append("  (keep training \u2014 skills unlock at 60% mastery)\n");
        for (String s : skills) sb.append("  \u2022 ").append(s).append("\n");
        sb.append("\nCERTIFICATES\n");
        boolean anyCert = false;
        for (String pid : pathCompleted) {
            LearningPath p = path(pid);
            if (p != null) { sb.append("  \u2022 ").append(p.certTitle).append("\n"); anyCert = true; }
        }
        if (!anyCert) sb.append("  (none yet)\n");
        sb.append("\nACHIEVEMENTS / BADGES\n");
        boolean anyBadge = false;
        for (EliteMission m : missions()) {
            if (isMissionDone(m.id)) { sb.append("  \u2022 ").append(m.badge).append("\n"); anyBadge = true; }
        }
        if (examResults.values().stream().anyMatch(r -> r.pass)) {
            sb.append("  \u2022 Proctored Exam Pass\n");
            anyBadge = true;
        }
        if (!anyBadge) sb.append("  (none yet)\n");
        sb.append("\nPROJECTS & LABS\n");
        List<PortfolioItem> projs = getPortfolioItems();
        if (projs.isEmpty()) sb.append("  (portfolio empty \u2014 complete labs to auto-collect)\n");
        for (PortfolioItem pi : projs) {
            sb.append("  \u2022 ").append(pi.title);
            if (!pi.url.isEmpty()) sb.append(" \u2014 ").append(pi.url);
            sb.append("\n");
        }
        sb.append("\nPROFILE LINKS\n");
        sb.append("  GitHub: ").append(githubUrl.isEmpty() ? "not set" : githubUrl).append("\n");
        sb.append("  LinkedIn: ").append(linkedinUrl.isEmpty() ? "not set" : linkedinUrl).append("\n");
        return sb.toString();
    }

    public String resumeMarkdown() {
        return generateResume().replace(" \u2014 ", " — ").replace("==========================================", "---");
    }

    private String LoginScreenUserName() {
        try {
            Class<?> c = Class.forName("ui.LoginScreen");
            java.lang.reflect.Field f = c.getField("USERNAME");
            Object v = f.get(null);
            return v == null ? "Operative" : String.valueOf(v);
        } catch (Exception e) {
            return "Operative";
        }
    }

    // ================================================================
    // 16. REPUTATION SYSTEM
    // ================================================================

    public void addTrust(int amount) { trustPoints = Math.max(0, trustPoints + amount); save(); }
    public void addReputation(int amount) { reputationPoints = Math.max(0, reputationPoints + amount); save(); }
    public void addCommunityPoints(int amount) { communityPoints = Math.max(0, communityPoints + amount); save(); }

    public void rateMentor(int stars) {
        if (stars < 1 || stars > 5) return;
        mentorRating = mentorRating == 0 ? stars : Math.max(1, Math.min(5,
            Math.round((mentorRating + stars) / 2.0f)));
        addTrust(5);
        save();
    }

    public void rateContributor(int stars) {
        if (stars < 1 || stars > 5) return;
        contributorRating = contributorRating == 0 ? stars : Math.max(1, Math.min(5,
            Math.round((contributorRating + stars) / 2.0f)));
        addCommunityPoints(5);
        save();
    }

    public int getTrust() { return trustPoints; }
    public int getReputation() { return reputationPoints; }
    public int getCommunityPoints() { return communityPoints; }
    public int getMentorRating() { return mentorRating; }
    public int getContributorRating() { return contributorRating; }

    public int reputationScore() {
        return Math.min(1000,
            trustPoints + reputationPoints * 2 + communityPoints + mentorRating * 10 + contributorRating * 10);
    }

    public List<String[]> reputationRows() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Trust Points", String.valueOf(trustPoints)});
        rows.add(new String[]{"Reputation", String.valueOf(reputationPoints)});
        rows.add(new String[]{"Community Points", String.valueOf(communityPoints)});
        rows.add(new String[]{"Mentor Rating", mentorRating == 0 ? "not rated" : mentorRating + " / 5"});
        rows.add(new String[]{"Contributor Rating", contributorRating == 0 ? "not rated" : contributorRating + " / 5"});
        rows.add(new String[]{"Overall Score", String.valueOf(reputationScore())});
        return rows;
    }

    // ================================================================
    // 17. COMMUNITY HUB
    // ================================================================

    public Board createBoard(String title, String descr) {
        boardCounter++;
        Board b = new Board("board_" + boardCounter, title, descr == null ? "" : descr, System.currentTimeMillis());
        boards.put(b.id, b);
        addCommunityPoints(3);
        save();
        return b;
    }

    public List<Board> getBoards() { return new ArrayList<>(boards.values()); }

    public Board getBoard(String id) { return boards.get(id); }

    public Thread postThread(String boardId, String title, String body) {
        Board b = boards.get(boardId);
        if (b == null) return null;
        Thread t = new Thread("thread_" + (b.threads.size() + 1), operatorId, title);
        t.body = body == null ? "" : body;
        b.threads.add(t);
        addCommunityPoints(2);
        save();
        return t;
    }

    public Thread commentOnThread(String boardId, int threadIdx, String comment) {
        Board b = boards.get(boardId);
        if (b == null || threadIdx < 0 || threadIdx >= b.threads.size()) return null;
        Thread t = b.threads.get(threadIdx);
        t.comments.add(operatorId + ": " + comment);
        addCommunityPoints(1);
        save();
        return t;
    }

    public Thread likeThread(String boardId, int threadIdx) {
        Board b = boards.get(boardId);
        if (b == null || threadIdx < 0 || threadIdx >= b.threads.size()) return null;
        Thread t = b.threads.get(threadIdx);
        t.likes++;
        addCommunityPoints(1);
        save();
        return t;
    }

    public Writeup createWriteup(String title, String body) {
        writeupCounter++;
        Writeup w = new Writeup("writeup_" + writeupCounter, title, operatorId, System.currentTimeMillis());
        w.body = body == null ? "" : body;
        writeups.put(w.id, w);
        addCommunityPoints(5);
        save();
        return w;
    }

    public List<Writeup> getWriteups() { return new ArrayList<>(writeups.values()); }

    public Writeup commentWriteup(String writeupId, String note) {
        Writeup w = writeups.get(writeupId);
        if (w == null) return null;
        w.comments.add(operatorId + ": " + note);
        save();
        return w;
    }

    public Writeup reviewWriteup(String writeupId, int stars, String note) {
        Writeup w = writeups.get(writeupId);
        if (w == null || stars < 1 || stars > 5) return null;
        w.reviews.add(stars + "\u2605 " + operatorId + ": " + (note == null ? "" : note));
        w.score = w.score == 0 ? stars : Math.max(1, Math.min(5, Math.round((w.score + stars) / 2.0f)));
        rateContributor(stars);
        save();
        return w;
    }

    // ================================================================
    // 18. PLUGIN SDK
    // ================================================================

    public Plugin registerPlugin(String name, String author, String version, String kind, String descr, String paramsCsv) {
        pluginCounter++;
        Plugin p = new Plugin("plugin_" + pluginCounter, name == null ? "Unnamed" : name,
            author == null ? "Anonymous" : author, version == null ? "1.0" : version,
            kind == null ? "CIPHER" : kind, descr == null ? "" : descr, System.currentTimeMillis());
        if (paramsCsv != null) {
            for (String pair : paramsCsv.split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) p.params.put(kv[0].trim(), kv[1].trim());
            }
        }
        p.enabled = true;
        plugins.put(p.id, p);
        notify("SYSTEM", "PLUGIN REGISTERED", p.kind + " plugin \"" + p.name + "\" v" + p.version + " by " + p.author + ".");
        save();
        return p;
    }

    public List<Plugin> getPlugins() { return new ArrayList<>(plugins.values()); }

    public List<Plugin> getPlugins(String kind) {
        List<Plugin> out = new ArrayList<>();
        for (Plugin p : plugins.values()) {
            if (p.kind.equalsIgnoreCase(kind) && p.enabled) out.add(p);
        }
        return out;
    }

    public Plugin togglePlugin(String id) {
        Plugin p = plugins.get(id);
        if (p == null) return null;
        p.enabled = !p.enabled;
        save();
        return p;
    }

    public boolean deletePlugin(String id) {
        if (plugins.remove(id) != null) { save(); return true; }
        return false;
    }

    public String pluginParam(String id, String key) {
        Plugin p = plugins.get(id);
        return p == null ? "" : p.params.getOrDefault(key, "");
    }

    public int pluginCount() { return plugins.size(); }

    /** Third-party CHALLENGE plugins carry their flag in params, verified without touching core code. */
    public boolean solvePluginChallenge(String pluginId, String flag) {
        Plugin p = plugins.get(pluginId);
        if (p == null || !p.enabled || !p.kind.equalsIgnoreCase("CHALLENGE")) return false;
        String expected = p.params.get("flag");
        if (expected == null || expected.isEmpty() || flag == null) return false;
        if (!expected.trim().equalsIgnoreCase(flag.trim())) return false;
        academy.awardXp(50);
        addReputation(5);
        addCommunityPoints(10);
        notify("ACHIEVEMENT", "PLUGIN CHALLENGE SOLVED", "\"" + p.name + "\" verified via the Plugin SDK.");
        save();
        return true;
    }

    // ================================================================
    // 19. API SDK
    // ================================================================

    private static final String[] API_ENDPOINTS = {
        "GET /api/v1/profile", "GET /api/v1/xp", "GET /api/v1/rank",
        "GET /api/v1/paths", "GET /api/v1/path/{id}", "GET /api/v1/missions",
        "GET /api/v1/teams", "GET /api/v1/certificates", "POST /api/v1/auth/token",
        "POST /api/v1/webhooks", "GET /api/v1/events", "GET /api/v1/soc/status"
    };

    public List<String> apiEndpoints() { return java.util.Arrays.asList(API_ENDPOINTS); }

    public ApiKey generateApiKey(String label) {
        apiKeyCounter++;
        String key = "uc_" + Integer.toHexString((operatorId + "|" + label + "|" + apiKeyCounter + "|" + System.nanoTime()).hashCode())
            + Long.toHexString(System.nanoTime());
        ApiKey ak = new ApiKey("key_" + apiKeyCounter, label == null ? "default" : label, key, System.currentTimeMillis());
        apiKeys.put(ak.id, ak);
        notify("SYSTEM", "API KEY CREATED", "Key \"" + ak.label + "\" ready to authenticate SDK calls.");
        save();
        return ak;
    }

    public List<ApiKey> apiKeys() { return new ArrayList<>(apiKeys.values()); }

    public ApiKey revokeApiKey(String id) {
        ApiKey ak = apiKeys.get(id);
        if (ak != null) { ak.revoked = true; save(); }
        return ak;
    }

    public String apiCall(String path, String apiKeyId) {
        ApiKey ak = apiKeys.get(apiKeyId);
        if (ak == null || ak.revoked) return "{\"error\":\"401 unauthorized\"}";
        ak.lastUsed = System.currentTimeMillis();
        save();
        if (path == null) return "{\"error\":\"404 not found\"}";
        String data;
        switch (path) {
            case "/api/v1/profile": data = LoginScreenUserName() + " (" + operatorId + ")"; break;
            case "/api/v1/xp": data = "xp=" + academy.getTotalXp(); break;
            case "/api/v1/rank": data = "trust=" + trustPoints + " rep=" + reputationPoints; break;
            case "/api/v1/paths": data = paths().size() + " paths"; break;
            case "/api/v1/missions": data = missions().size() + " missions"; break;
            case "/api/v1/soc/status": data = socStatus().replace("\n", " | "); break;
            default: data = "ok";
        }
        return "{\"ok\":true,\"path\":\"" + path + "\",\"data\":\"" + data + "\"}";
    }

    public Webhook registerWebhook(String url, String event) {
        webhookCounter++;
        Webhook w = new Webhook("wh_" + webhookCounter, url == null ? "" : url,
            event == null ? "*" : event, System.currentTimeMillis());
        webhooks.put(w.id, w);
        save();
        return w;
    }

    public List<Webhook> webhooks() { return new ArrayList<>(webhooks.values()); }

    public boolean deleteWebhook(String id) {
        if (webhooks.remove(id) != null) { save(); return true; }
        return false;
    }

    public int dispatchWebhook(String event, String payload) {
        int fired = 0;
        for (Webhook w : webhooks.values()) {
            if (w.event.equals("*") || w.event.equalsIgnoreCase(event)) {
                webhookLog.add(System.currentTimeMillis() + " " + event + " -> " + w.url + " " + payload);
                fired++;
            }
        }
        if (fired > 0) save();
        return fired;
    }

    public List<String> webhookLog() { return new ArrayList<>(webhookLog); }

    public String sdkDoc() {
        StringBuilder sb = new StringBuilder();
        sb.append("UC-FORTRESS API SDK v3\n");
        sb.append("Auth header: X-API-Key <key>\n\n");
        sb.append("REST ENDPOINTS\n");
        for (String e : API_ENDPOINTS) sb.append("  ").append(e).append("\n");
        sb.append("\nJAVA SDK\n");
        sb.append("  UcClient client = new UcClient(apiKey);\n");
        sb.append("  String res = client.get(\"/api/v1/profile\");\n\n");
        sb.append("PYTHON SDK\n");
        sb.append("  from ucsuite import Client\n");
        sb.append("  c = Client(api_key)\n");
        sb.append("  print(c.get(\"/api/v1/profile\"))\n\n");
        sb.append("WEBHOOKS\n");
        sb.append("  POST /api/v1/webhooks {url, event}  -> fires on matching events\n");
        return sb.toString();
    }

    // ================================================================
    // 20. MARKETPLACE
    // ================================================================

    public MarketListing publishListing(String kind, String title, String descr, int price) {
        marketCounter++;
        MarketListing l = new MarketListing("listing_" + marketCounter,
            kind == null ? "COURSE" : kind, title, descr == null ? "" : descr,
            Math.max(0, price), operatorId, System.currentTimeMillis());
        market.put(l.id, l);
        notify("MARKETPLACE", "LISTING PUBLISHED", "\"" + l.title + "\" (" + l.kind + ") is live on the marketplace.");
        addCommunityPoints(3);
        save();
        return l;
    }

    public List<MarketListing> marketListings(String kind) {
        List<MarketListing> out = new ArrayList<>();
        for (MarketListing l : market.values()) {
            if (kind == null || l.kind.equalsIgnoreCase(kind)) out.add(l);
        }
        return out;
    }

    public MarketListing getListing(String id) { return market.get(id); }

    public boolean purchaseListing(String id) {
        MarketListing l = market.get(id);
        if (l == null || purchases.contains(id)) return false;
        if (academy.getCoins() < l.price) return false;
        academy.addCoins(-l.price);
        purchases.add(id);
        l.purchasedCount++;
        save();
        return true;
    }

    public boolean hasPurchased(String id) { return purchases.contains(id); }

    public Set<String> myPurchases() { return new LinkedHashSet<>(purchases); }

    public MarketListing rateListing(String id, int stars) {
        MarketListing l = market.get(id);
        if (l == null || stars < 1 || stars > 5) return null;
        l.ratingSum += stars;
        l.reviewCount++;
        l.rating = Math.round(l.ratingSum * 10.0f / l.reviewCount) / 10.0f;
        rateContributor(stars);
        save();
        return l;
    }

    public boolean deleteListing(String id) {
        if (market.remove(id) != null) { save(); return true; }
        return false;
    }

    // ================================================================
    // 21. OFFLINE MODE
    // ================================================================

    public boolean isOfflineMode() { return offlineMode; }

    public void setOfflineMode(boolean on) {
        offlineMode = on;
        if (!on) syncNow();
        save();
    }

    public void enqueueOffline(String action) {
        offlineQueue.add(System.currentTimeMillis() + " " + (action == null ? "" : action));
        save();
    }

    public List<String> offlineQueue() { return new ArrayList<>(offlineQueue); }

    public int syncNow() {
        int n = offlineQueue.size();
        for (String a : offlineQueue) syncLog.add("SYNCED " + a);
        if (n > 0) lastSyncAt = System.currentTimeMillis();
        offlineQueue.clear();
        save();
        return n;
    }

    public String offlineStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(offlineMode ? "OFFLINE MODE ACTIVE \u2014 academy runs without internet" : "ONLINE MODE").append("\n");
        sb.append("Pending actions: ").append(offlineQueue.size()).append("\n");
        sb.append("Last sync: ").append(lastSyncAt == 0 ? "never" : new java.util.Date(lastSyncAt).toString()).append("\n");
        sb.append("Sync log entries: ").append(syncLog.size()).append("\n");
        return sb.toString();
    }

    // ================================================================
    // 22. CLOUD SYNC
    // ================================================================

    public boolean isCloudEnabled() { return cloudEnabled; }

    public void setCloudEnabled(boolean on) { cloudEnabled = on; save(); }

    public Device createDevice(String name) {
        deviceCounter++;
        Device d = new Device("dev_" + deviceCounter, name == null ? "device" : name, System.currentTimeMillis());
        devices.put(d.id, d);
        save();
        return d;
    }

    public List<Device> devices() { return new ArrayList<>(devices.values()); }

    public Device pushToCloud(String deviceId) {
        Device d = devices.get(deviceId);
        if (d == null || !cloudEnabled) return null;
        String snap = snapshot();
        if (!d.snapshot.isEmpty() && !d.snapshot.equals(snap)) cloudConflicts++;
        d.snapshot = snap;
        d.hash = Integer.toHexString(snap.hashCode());
        d.lastSync = System.currentTimeMillis();
        lastCloudSync = d.lastSync;
        save();
        return d;
    }

    public int pullFromCloud(String deviceId) {
        Device d = devices.get(deviceId);
        if (d == null || d.snapshot.isEmpty()) return 0;
        try {
            apply(new JSONObject(d.snapshot));
            save();
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    public int resolveConflict(String deviceId, boolean preferLocal) {
        Device d = devices.get(deviceId);
        if (d == null) return cloudConflicts;
        cloudConflicts = Math.max(0, cloudConflicts - 1);
        if (preferLocal) {
            d.snapshot = snapshot();
            d.hash = Integer.toHexString(d.snapshot.hashCode());
        }
        save();
        return cloudConflicts;
    }

    public String cloudStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(cloudEnabled ? "CLOUD SYNC ENABLED \u2014 encrypted cross-device backup" : "CLOUD SYNC DISABLED").append("\n");
        sb.append("Devices: ").append(devices.size()).append("\n");
        sb.append("Conflicts awaiting resolution: ").append(cloudConflicts).append("\n");
        sb.append("Last cloud sync: ").append(lastCloudSync == 0 ? "never" : new java.util.Date(lastCloudSync).toString()).append("\n");
        return sb.toString();
    }

    private String snapshot() {
        try { return toJson().toString(); } catch (Exception e) { return "{}"; }
    }

    // ================================================================
    // 23. SECURITY OPERATIONS CENTER
    // ================================================================

    public SocAlert raiseAlert(String severity, String source, String message) {
        socAlertCounter++;
        SocAlert a = new SocAlert("alert_" + socAlertCounter,
            severity == null ? "LOW" : severity, source == null ? "sensor" : source,
            message == null ? "" : message, System.currentTimeMillis());
        socAlerts.put(a.id, a);
        timeline.add(a.ts + " ALERT [" + a.severity + "] " + a.source + ": " + a.message);
        save();
        return a;
    }

    public List<SocAlert> alerts() { return new ArrayList<>(socAlerts.values()); }

    public SocAlert acknowledgeAlert(String id) {
        SocAlert a = socAlerts.get(id);
        if (a != null) { a.acknowledged = true; save(); }
        return a;
    }

    public Incident createIncident(String title, String severity, String descr) {
        incidentCounter++;
        Incident in = new Incident("inc_" + incidentCounter, title,
            severity == null ? "HIGH" : severity, descr == null ? "" : descr,
            System.currentTimeMillis(), "OPEN");
        incidents.put(in.id, in);
        timeline.add(in.ts + " INCIDENT [" + in.severity + "] " + in.title + " opened");
        save();
        return in;
    }

    public List<Incident> incidents() { return new ArrayList<>(incidents.values()); }

    public Incident respondToIncident(String id, String action) {
        Incident in = incidents.get(id);
        if (in == null) return null;
        in.responseLog.add(action == null ? "" : action);
        in.status = "INVESTIGATING";
        timeline.add(System.currentTimeMillis() + " RESPONSE " + in.id + ": " + action);
        if (in.responseLog.size() >= 3) in.status = "RESOLVED";
        save();
        return in;
    }

    public List<String> threatFeed() {
        return java.util.Arrays.asList(
            "PHISHING CAMPAIGN \u2014 mass credential harvesting (SEVERE)",
            "RANSOMWARE \u2014 new family observed in the wild (CRITICAL)",
            "CRYPTO-MINING \u2014 container breakout attempts (HIGH)",
            "ZERO-DAY \u2014 XSS in legacy portal (HIGH)",
            "DDoS \u2014 amplification attacks on DNS (MEDIUM)",
            "INSIDER \u2014 unusual data exfiltration pattern (MEDIUM)");
    }

    public List<String> attackTimeline() { return new ArrayList<>(timeline); }

    public String socStatus() {
        StringBuilder sb = new StringBuilder();
        long openAlerts = 0;
        for (SocAlert a : socAlerts.values()) if (!a.acknowledged) openAlerts++;
        long openInc = 0;
        for (Incident i : incidents.values()) if (!i.status.equals("RESOLVED")) openInc++;
        sb.append("Active alerts: ").append(openAlerts).append("\n");
        sb.append("Open incidents: ").append(openInc).append("\n");
        sb.append("Threat feed entries: ").append(threatFeed().size()).append("\n");
        sb.append("Timeline events: ").append(timeline.size()).append("\n");
        return sb.toString();
    }

    // ================================================================
    // 24. QUANTUM CRYPTOGRAPHY
    // ================================================================

    private static final String[][] QUANTUM_TOPICS = {
        {"BB84", "Bennett-Brassard 1984 protocol \u2014 transmits a secret key using polarized photons."},
        {"QKD", "Quantum Key Distribution \u2014 eavesdropping is detected via measurement disturbance."},
        {"POST-QUANTUM", "Cryptography designed to survive attacks from quantum computers (Shor's algorithm)."},
        {"LATTICE", "Hard lattice problems (LWE/SIS) underpin most post-quantum schemes."},
        {"KYBER", "CRYSTALS-Kyber \u2014 the ML-KEM selected for NIST standardization."},
        {"DILITHIUM", "CRYSTALS-Dilithium \u2014 a lattice-based digital signature scheme."}
    };

    private static final String[][] QUANTUM_QUIZ = {
        {"In BB84, which basis pair is NOT used for key bits?", "Rectilinear", "Diagonal", "Time", "Both A and B", "2"},
        {"What does QKD detect?", "Replay attacks", "Eavesdroppers", "Malware", "Phishing", "1"},
        {"Shor's algorithm breaks which classical scheme?", "AES-256", "SHA-256", "RSA", "None", "2"},
        {"Which problem underpins CRYSTALS-Kyber?", "Lattice", "Discrete log", "Graph", "Merkle", "0"},
        {"CRYSTALS-Dilithium is a post-quantum ____.", "block cipher", "signature scheme", "hash", "stream cipher", "1"}
    };

    public List<String[]> quantumTopics() { return java.util.Arrays.asList(QUANTUM_TOPICS); }

    public boolean learnQuantum(String topic) {
        if (topic == null) return false;
        for (String[] t : QUANTUM_TOPICS) {
            if (t[0].equalsIgnoreCase(topic)) {
                if (quantumLearned.add(t[0])) {
                    academy.awardXp(30);
                    notify("ACHIEVEMENT", "QUANTUM TOPIC LEARNED", t[0] + " \u2014 " + t[1]);
                    save();
                }
                return true;
            }
        }
        return false;
    }

    public boolean isQuantumLearned(String topic) { return topic != null && quantumLearned.contains(topic.toUpperCase()); }

    public double quantumMastery() { return quantumLearned.size() * 100.0 / Math.max(1, QUANTUM_TOPICS.length); }

    public List<ExamQuestion> quantumQuizQuestions() {
        List<ExamQuestion> out = new ArrayList<>();
        for (String[] q : QUANTUM_QUIZ) {
            out.add(new ExamQuestion(q[0], new String[]{q[1], q[2], q[3], q[4]}, Integer.parseInt(q[5])));
        }
        return out;
    }

    public int submitQuantumQuiz(List<Integer> answers) {
        List<ExamQuestion> qs = quantumQuizQuestions();
        int correct = 0;
        for (int i = 0; i < qs.size(); i++) {
            if (i < answers.size() && answers.get(i) != null && answers.get(i) == qs.get(i).answerIdx) correct++;
        }
        int pct = (int) Math.round(correct * 100.0 / Math.max(1, qs.size()));
        if (pct >= 60) academy.awardXp(40);
        save();
        return pct;
    }

    // ================================================================
    // 25. BLOCKCHAIN SECURITY
    // ================================================================

    public Wallet createWallet(String name) {
        walletCounter++;
        String raw = Integer.toHexString((operatorId + "|" + name + "|" + walletCounter + "|" + System.nanoTime()).hashCode());
        if (raw.length() < 8) raw = ("00000000" + raw).substring(raw.length());
        Wallet w = new Wallet("wallet_" + walletCounter, name == null ? "wallet" : name,
            "0x" + raw.substring(0, 8).toUpperCase(), 100);
        wallets.put(w.id, w);
        save();
        return w;
    }

    public List<Wallet> wallets() { return new ArrayList<>(wallets.values()); }

    public String signData(String walletId, String data) {
        Wallet w = wallets.get(walletId);
        return w == null || data == null ? "" : AcademyService.sha256Hex(w.address + ":" + data).substring(0, 32);
    }

    public boolean verifySignature(String walletId, String data, String sig) {
        String expected = signData(walletId, data);
        return !expected.isEmpty() && expected.equalsIgnoreCase(sig);
    }

    public Block addBlock(String data) {
        int index = chain.size();
        String prevHash = chain.isEmpty() ? repeatZero(64) : chain.get(String.valueOf(index - 1)).hash;
        long ts = System.currentTimeMillis();
        int nonce = 0;
        String h;
        do {
            nonce++;
            h = AcademyService.sha256Hex(index + "|" + prevHash + "|" + data + "|" + ts + "|" + nonce);
        } while (!h.startsWith("00") && nonce < 50000);
        Block b = new Block(index, prevHash, h, data == null ? "" : data, ts, nonce);
        chain.put(String.valueOf(index), b);
        save();
        return b;
    }

    public List<Block> blockchain() { return new ArrayList<>(chain.values()); }

    public boolean verifyChain() {
        for (int i = 1; i < chain.size(); i++) {
            Block cur = chain.get(String.valueOf(i));
            Block prev = chain.get(String.valueOf(i - 1));
            if (cur == null || prev == null || !cur.prevHash.equals(prev.hash)) return false;
        }
        return true;
    }

    public String[] merkleTree(List<String> items) {
        if (items == null || items.isEmpty()) return new String[]{repeatZero(64), "0"};
        List<String> level = new ArrayList<>();
        for (String it : items) level.add(AcademyService.sha256Hex(it));
        int rounds = 0;
        while (level.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                String l = level.get(i);
                String r = i + 1 < level.size() ? level.get(i + 1) : l;
                next.add(AcademyService.sha256Hex(l + r));
            }
            level = next;
            rounds++;
        }
        return new String[]{level.get(0), String.valueOf(rounds)};
    }

    private static String repeatZero(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('0');
        return sb.toString();
    }

    public Contract deployContract(String name, String code) {
        contractCounter++;
        Contract c = new Contract("contract_" + contractCounter,
            name == null ? "contract" : name, code == null ? "{}" : code);
        contracts.put(c.id, c);
        save();
        return c;
    }

    public List<Contract> contracts() { return new ArrayList<>(contracts.values()); }

    public Contract callContract(String id) {
        Contract c = contracts.get(id);
        if (c != null) { c.calls++; save(); }
        return c;
    }

    public String consensusStats() {
        return "CONSENSUS ENGINE\n" +
            "  Proof-of-Work \u2022 hash prefix \"00\" (difficulty ~16)\n" +
            "  Longest chain rule \u2022 tamper detection via verifyChain()\n" +
            "  Blocks mined: " + chain.size() + " \u2022 chain valid: " + verifyChain();
    }

    // ================================================================
    // 26. MACHINE LEARNING SECURITY
    // ================================================================

    public String mlDetect(String kind, String features) {
        String k = kind == null ? "FRAUD" : kind;
        double score = 0.5 + ((k + ":" + features).hashCode() % 1000) / 2000.0;
        boolean flag = score > 0.75;
        if (flag) mlFlagged++;
        mlLog.add(System.currentTimeMillis() + " " + k + " features=" + features +
            " score=" + (int) (score * 100) + "% " + (flag ? "FLAGGED" : "OK"));
        save();
        return (flag ? "FLAGGED" : "CLEAR") + " [" + k + "] confidence " + (int) (score * 100) + "%";
    }

    public String adversarialSample(String kind) {
        String k = kind == null ? "MALWARE" : kind;
        mlDetect(k, "benign-" + rnd.nextInt(1000));
        String msg = "Adversarial perturbation bypassed the " + k + " detector confidence by ~28%.";
        mlLog.add(System.currentTimeMillis() + " ADVERSARIAL " + k + " " + msg);
        save();
        return msg;
    }

    public List<String> mlLog() { return new ArrayList<>(mlLog); }

    public String mlReport() {
        return "ML SECURITY REPORT\n" +
            "  Detections logged: " + mlLog.size() + "\n" +
            "  Flagged samples: " + mlFlagged + "\n" +
            "  Detectors: FRAUD, MALWARE, PHISHING, ANOMALY\n" +
            "  Defense: model hardening, adversarial retraining, anomaly baselines";
    }

    // ================================================================
    // 27. DIGITAL FORENSICS WORKBENCH
    // ================================================================

    public ForensicCase createCase(String title) {
        caseCounter++;
        ForensicCase fc = new ForensicCase("case_" + caseCounter, title == null ? "case" : title,
            System.currentTimeMillis());
        forensicCases.put(fc.id, fc);
        save();
        return fc;
    }

    public List<ForensicCase> forensicCases() { return new ArrayList<>(forensicCases.values()); }

    public ForensicCase addEvidence(String caseId, String kind, String artifact) {
        ForensicCase fc = forensicCases.get(caseId);
        if (fc == null) return null;
        fc.evidence.add(kind == null ? "DISK" : kind);
        fc.evidence.add(artifact == null ? "" : artifact);
        save();
        return fc;
    }

    public ForensicCase runAnalysis(String caseId) {
        ForensicCase fc = forensicCases.get(caseId);
        if (fc == null) return null;
        fc.timeline.clear();
        fc.chain.clear();
        String prev = repeatZero(64);
        for (int i = 0; i + 1 < fc.evidence.size(); i += 2) {
            String entry = fc.evidence.get(i) + ":" + fc.evidence.get(i + 1);
            String h = AcademyService.sha256Hex(prev + entry);
            fc.chain.add(h);
            prev = h;
            fc.timeline.add("T+" + (i / 2 + 1) + " " + fc.evidence.get(i) + " artifact hashed " + h.substring(0, 12) + "...");
        }
        fc.status = "COMPLETE";
        save();
        return fc;
    }

    public String caseReport(String caseId) {
        ForensicCase fc = forensicCases.get(caseId);
        if (fc == null) return "case not found";
        StringBuilder sb = new StringBuilder();
        sb.append("FORENSIC CASE ").append(fc.id).append(" \u2014 ").append(fc.title)
          .append(" [").append(fc.status).append("]\n");
        sb.append("Evidence artifacts: ").append(fc.evidence.size() / 2).append("\n");
        sb.append("Timeline:\n");
        for (String t : fc.timeline) sb.append("  ").append(t).append("\n");
        sb.append("Evidence chain: ").append(fc.chain.isEmpty() ? "n/a"
            : "linked (" + fc.chain.size() + " hashes)").append("\n");
        return sb.toString();
    }

    public List<String> evidenceChain(String caseId) {
        ForensicCase fc = forensicCases.get(caseId);
        return fc == null ? new ArrayList<>() : new ArrayList<>(fc.chain);
    }

    // ================================================================
    // 28. GLOBAL EVENTS
    // ================================================================

    public GlobalEvent createEvent(String title, String kind, int xpReward) {
        eventCounter++;
        long now = System.currentTimeMillis();
        GlobalEvent ge = new GlobalEvent("event_" + eventCounter, title,
            kind == null ? "WEEKLY" : kind, now, now + 7L * 24 * 3600 * 1000, Math.max(0, xpReward));
        globalEvents.put(ge.id, ge);
        save();
        return ge;
    }

    public List<GlobalEvent> events() { return new ArrayList<>(globalEvents.values()); }

    public boolean joinEvent(String id) {
        GlobalEvent ge = globalEvents.get(id);
        if (ge == null || eventJoined.contains(id)) return false;
        eventJoined.add(id);
        addCommunityPoints(2);
        save();
        return true;
    }

    public boolean submitEventScore(String id, int score) {
        GlobalEvent ge = globalEvents.get(id);
        if (ge == null) return false;
        eventScores.put(id, Math.max(eventScores.getOrDefault(id, 0), score));
        academy.awardXp(score > 70 ? ge.xpReward : Math.max(1, ge.xpReward / 2));
        save();
        return true;
    }

    public int eventScore(String id) { return eventScores.getOrDefault(id, 0); }

    public boolean isEventJoined(String id) { return eventJoined.contains(id); }

    public List<String[]> eventLeaderboard() {
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> e : eventScores.entrySet()) {
            GlobalEvent ge = globalEvents.get(e.getKey());
            rows.add(new String[]{ge == null ? e.getKey() : ge.title, String.valueOf(e.getValue())});
        }
        rows.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
        return rows;
    }

    // ================================================================
    // 29. ENTERPRISE REPORTS
    // ================================================================

    public List<String[]> skillMatrix() {
        List<String[]> rows = new ArrayList<>();
        for (String t : CURRICULUM_TOPICS) {
            rows.add(new String[]{t, (int) Math.round(mastery(t)) + "%"});
        }
        return rows;
    }

    public String enterpriseReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("UC-FORTRESS ACADEMY \u2014 ENTERPRISE REPORT\n");
        sb.append("==========================================\n");
        sb.append("Operator: ").append(LoginScreenUserName()).append("\n");
        sb.append("Total XP: ").append(academy.getTotalXp()).append(" \u2022 Coins: ").append(academy.getCoins()).append("\n\n");
        sb.append("COMPLETION\n");
        sb.append("  Paths completed: ").append(pathCompleted.size()).append("/").append(paths().size()).append("\n");
        sb.append("  Missions done: ").append(missionDone.size()).append("/").append(missions().size()).append("\n");
        sb.append("  Exams passed: ").append(examPassCount()).append("\n\n");
        sb.append("SKILL MATRIX\n");
        for (String[] r : skillMatrix()) sb.append("  \u2022 ").append(r[0]).append(" \u2014 ").append(r[1]).append("\n");
        sb.append("\nWEAK AREAS\n");
        for (String[] w : detectWeakTopics()) sb.append("  \u2022 ").append(w[0]).append(" \u2014 ").append(w[1]).append("%\n");
        sb.append("\nRECOMMENDATIONS\n");
        for (String r : recommendLessons()) sb.append("  \u2022 ").append(r).append("\n");
        reportLog.add(System.currentTimeMillis() + " enterprise report generated");
        save();
        return sb.toString();
    }

    private int examPassCount() {
        int n = 0;
        for (ExamResult r : examResults.values()) if (r.pass) n++;
        return n;
    }

    public File exportPdfReport() {
        try {
            File dir = new File(APP_DIR);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(APP_DIR + File.separator + "enterprise_report_" + System.currentTimeMillis() + ".pdf");
            Files.writeString(f.toPath(), enterpriseReport());
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    // 30. FUTURE-READY ARCHITECTURE
    // ================================================================

    public ArchComponent registerComponent(String name, String target) {
        String base = "arch_" + (name == null ? "comp" : name.toLowerCase().replaceAll("[^a-z0-9]", "_"));
        String id = base + (architecture.size() + 1);
        ArchComponent ac = new ArchComponent(id, name == null ? "Component" : name,
            target == null ? "CLOUD" : target, "READY", true);
        architecture.put(id, ac);
        save();
        return ac;
    }

    public List<ArchComponent> components() { return new ArrayList<>(architecture.values()); }

    public String architectureReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("FUTURE-READY ARCHITECTURE\n");
        sb.append("Every subsystem is interface-driven and swappable.\n\n");
        for (ArchComponent ac : architecture.values()) {
            sb.append("  \u2022 ").append(ac.name).append(" [").append(ac.status)
              .append("] \u2192 ").append(ac.target)
              .append(ac.interfaceBound ? " (interface-bound)" : "").append("\n");
        }
        return sb.toString();
    }

    // ================================================================
    // DATA HOLDERS
    // ================================================================

    public static final class LearningPath {
        public final String id, title, icon, level, descr, certTitle;
        public final int estHours;
        public final List<String> prereqs, lessons, labs, ctfs, projects;
        public final int examQuestions;
        public LearningPath(String id, String title, String icon, String level, int estHours, String descr,
                            String certTitle, List<String> prereqs, List<String> lessons, List<String> labs,
                            List<String> ctfs, List<String> projects, int examQuestions) {
            this.id = id; this.title = title; this.icon = icon; this.level = level;
            this.estHours = estHours; this.descr = descr; this.certTitle = certTitle;
            this.prereqs = prereqs; this.lessons = lessons; this.labs = labs;
            this.ctfs = ctfs; this.projects = projects; this.examQuestions = examQuestions;
        }
    }

    public static final class PathProgress {
        public final String pathId;
        public int lessonsDone, labsDone, ctfsDone, projectsDone, examScore;
        public PathProgress(String pathId, int lessons, int labs, int ctfs, int projects, int exam) {
            this.pathId = pathId;
            this.lessonsDone = lessons; this.labsDone = labs; this.ctfsDone = ctfs;
            this.projectsDone = projects; this.examScore = exam;
        }
        public int totalItems() { return lessonsDone + labsDone + ctfsDone + projectsDone + (examScore >= 70 ? 1 : 0); }
    }

    public static final class RangeStep {
        public final String prompt, question, answer;
        public final int xp;
        public RangeStep(String prompt, String question, String answer, int xp) {
            this.prompt = prompt; this.question = question; this.answer = answer; this.xp = xp;
        }
    }

    public static final class RangeEnv {
        public final String id, name, icon, threat, descr;
        public final List<RangeStep> steps;
        public RangeEnv(String id, String name, String icon, String threat, String descr, List<RangeStep> steps) {
            this.id = id; this.name = name; this.icon = icon; this.threat = threat;
            this.descr = descr; this.steps = steps;
        }
    }

    public static final class MissionObjective {
        public final String id, text, question, answer;
        public final int xp;
        public MissionObjective(String id, String text, String question, String answer, int xp) {
            this.id = id; this.text = text; this.question = question; this.answer = answer; this.xp = xp;
        }
    }

    public static final class EliteMission {
        public final String id, title, icon, brief, badge;
        public final List<MissionObjective> objectives;
        public final int xpReward, coins;
        public EliteMission(String id, String title, String icon, String brief, List<MissionObjective> objectives,
                            int xpReward, int coins, String badge) {
            this.id = id; this.title = title; this.icon = icon; this.brief = brief;
            this.objectives = objectives; this.xpReward = xpReward; this.coins = coins; this.badge = badge;
        }
    }

    public static final class StoryScene {
        public final String title, prompt, question, answer;
        public final int xp;
        public StoryScene(String title, String prompt, String question, String answer, int xp) {
            this.title = title; this.prompt = prompt; this.question = question; this.answer = answer; this.xp = xp;
        }
    }

    public static final class StoryEpisode {
        public final String id, title, icon, summary;
        public final int part;
        public final List<StoryScene> scenes;
        public StoryEpisode(String id, String title, String icon, int part, String summary, List<StoryScene> scenes) {
            this.id = id; this.title = title; this.icon = icon; this.part = part;
            this.summary = summary; this.scenes = scenes;
        }
    }

    public static final class Team {
        public final String id, name, motto;
        public final long created;
        public final Map<String, Integer> members = new LinkedHashMap<>();
        public final List<String> chat = new ArrayList<>();
        public String challengeTitle = "", challengeDescr = "", challengeAnswer = "";
        public int challengeXp;
        public boolean challengeSolved, certIssued;
        public Team(String id, String name, String motto, long created) {
            this.id = id; this.name = name; this.motto = motto; this.created = created;
        }
        public int sharedXp() {
            int sum = 0;
            for (int v : members.values()) sum += v;
            return sum;
        }
    }

    public static final class Course {
        public final String id, title, descr;
        public final long created;
        public final List<String> students = new ArrayList<>();
        public final List<String> assignments = new ArrayList<>();
        public final List<String> certs = new ArrayList<>();
        public Course(String id, String title, String descr, long created) {
            this.id = id; this.title = title; this.descr = descr; this.created = created;
        }
    }

    public static final class StudentRecord {
        public final String name;
        public int xp, activeDays;
        public StudentRecord(String name, int xp, int activeDays) {
            this.name = name; this.xp = xp; this.activeDays = activeDays;
        }
    }

    public static final class Department {
        public final String id, name;
        public final List<String> classNames = new ArrayList<>();
        public final Map<String, List<String>> classStudents = new LinkedHashMap<>();
        public final List<UniExam> exams = new ArrayList<>();
        public final List<String> assignments = new ArrayList<>();
        public Department(String id, String name) {
            this.id = id; this.name = name;
        }
    }

    public static final class UniExam {
        public final String id, deptId, className, title;
        public final int marks;
        public final Map<String, Integer> scores = new LinkedHashMap<>();
        public UniExam(String id, String deptId, String className, String title, int marks) {
            this.id = id; this.deptId = deptId; this.className = className; this.title = title; this.marks = marks;
        }
    }

    public static final class Employee {
        public final String name, role;
        public final Map<String, Boolean> modules = new LinkedHashMap<>();
        public final Map<String, Boolean> compliance = new LinkedHashMap<>();
        public Employee(String name, String role) {
            this.name = name; this.role = role;
        }
    }

    public static final class Company {
        public final String id, name;
        public final List<Employee> employees = new ArrayList<>();
        public final List<String> reports = new ArrayList<>();
        public Company(String id, String name) {
            this.id = id; this.name = name;
        }
    }

    public static final class ExamQuestion {
        public final String prompt;
        public final String[] options;
        public final int answerIdx;
        public ExamQuestion(String prompt, String[] options, int answerIdx) {
            this.prompt = prompt; this.options = options; this.answerIdx = answerIdx;
        }
    }

    public static final class Exam {
        public final String id;
        public final List<ExamQuestion> questions;
        public final int durationSec;
        public Exam(String id, List<ExamQuestion> questions, int durationSec) {
            this.id = id; this.questions = questions; this.durationSec = durationSec;
        }
    }

    public static final class ExamResult {
        public final String examId;
        public final int score;
        public final boolean pass;
        public final long ts;
        public final boolean integrityOk;
        public ExamResult(String examId, int score, boolean pass, long ts, boolean integrityOk) {
            this.examId = examId; this.score = score; this.pass = pass;
            this.ts = ts; this.integrityOk = integrityOk;
        }
    }

    public static final class CustomLab {
        public final String id, type, title, prompt, answer, diff;
        public final int xp;
        public boolean solved;
        public CustomLab(String id, String type, String title, String prompt, String answer, int xp, String diff) {
            this.id = id; this.type = type; this.title = title; this.prompt = prompt;
            this.answer = answer; this.xp = xp; this.diff = diff;
        }
    }

    public static final class CustomCtf {
        public final String id, title, category, difficulty, description, solution;
        public final int xp, timerSec;
        public final long created;
        public final List<String> flags = new ArrayList<>();
        public final List<String> hints = new ArrayList<>();
        public final List<String> attachments = new ArrayList<>();
        public boolean solved;
        public long solvedAt;
        public CustomCtf(String id, String title, String category, String difficulty, String description,
                         String solution, int xp, int timerSec, long created) {
            this.id = id; this.title = title; this.category = category; this.difficulty = difficulty;
            this.description = description; this.solution = solution; this.xp = xp;
            this.timerSec = timerSec; this.created = created;
        }
    }

    public static final class NotebookEntry {
        public final String id, title, kind;
        public String body;
        public String tags = "";
        public final long created;
        public NotebookEntry(String id, String title, String kind, long created) {
            this.id = id; this.title = title; this.kind = kind; this.created = created;
        }
    }

    public static final class PortfolioItem {
        public final String id, kind, title;
        public String url = "", notes = "";
        public final long created;
        public PortfolioItem(String id, String kind, String title, long created) {
            this.id = id; this.kind = kind; this.title = title; this.created = created;
        }
    }

    public static final class Board {
        public final String id, title, descr;
        public final long created;
        public final List<Thread> threads = new ArrayList<>();
        public Board(String id, String title, String descr, long created) {
            this.id = id; this.title = title; this.descr = descr; this.created = created;
        }
    }

    public static final class Thread {
        public final String id, author, title;
        public String body = "";
        public int likes;
        public final List<String> comments = new ArrayList<>();
        public Thread(String id, String author, String title) {
            this.id = id; this.author = author; this.title = title;
        }
    }

    public static final class Writeup {
        public final String id, title, author;
        public final long created;
        public String body = "";
        public int score;
        public final List<String> reviews = new ArrayList<>();
        public final List<String> comments = new ArrayList<>();
        public Writeup(String id, String title, String author, long created) {
            this.id = id; this.title = title; this.author = author; this.created = created;
        }
    }

    public static final class Plugin {
        public final String id, name, author, version, kind, descr;
        public final long registeredAt;
        public boolean enabled;
        public final Map<String, String> params = new LinkedHashMap<>();
        public Plugin(String id, String name, String author, String version, String kind, String descr, long registeredAt) {
            this.id = id; this.name = name; this.author = author; this.version = version;
            this.kind = kind; this.descr = descr; this.registeredAt = registeredAt;
        }
    }

    public static final class ApiKey {
        public final String id, label, key;
        public final long created;
        public long lastUsed;
        public boolean revoked;
        public ApiKey(String id, String label, String key, long created) {
            this.id = id; this.label = label; this.key = key; this.created = created;
        }
    }

    public static final class Webhook {
        public final String id, url, event;
        public final long created;
        public Webhook(String id, String url, String event, long created) {
            this.id = id; this.url = url; this.event = event; this.created = created;
        }
    }

    public static final class MarketListing {
        public final String id, kind, title, descr, publisher;
        public final int price;
        public final long created;
        public double rating;
        public int reviewCount, ratingSum, purchasedCount;
        public MarketListing(String id, String kind, String title, String descr, int price,
                             String publisher, long created) {
            this.id = id; this.kind = kind; this.title = title; this.descr = descr;
            this.price = price; this.publisher = publisher; this.created = created;
        }
    }

    public static final class Device {
        public final String id, name;
        public final long created;
        public String snapshot = "", hash = "";
        public long lastSync;
        public Device(String id, String name, long created) {
            this.id = id; this.name = name; this.created = created;
        }
    }

    public static final class SocAlert {
        public final String id, severity, source, message;
        public final long ts;
        public boolean acknowledged;
        public SocAlert(String id, String severity, String source, String message, long ts) {
            this.id = id; this.severity = severity; this.source = source;
            this.message = message; this.ts = ts;
        }
    }

    public static final class Incident {
        public final String id, title, severity, descr;
        public final long ts;
        public String status;
        public final List<String> responseLog = new ArrayList<>();
        public Incident(String id, String title, String severity, String descr, long ts, String status) {
            this.id = id; this.title = title; this.severity = severity;
            this.descr = descr; this.ts = ts; this.status = status;
        }
    }

    public static final class Wallet {
        public final String id, name, address;
        public int balance;
        public Wallet(String id, String name, String address, int balance) {
            this.id = id; this.name = name; this.address = address; this.balance = balance;
        }
    }

    public static final class Block {
        public final int index, nonce;
        public final String prevHash, hash, data;
        public final long ts;
        public Block(int index, String prevHash, String hash, String data, long ts, int nonce) {
            this.index = index; this.prevHash = prevHash; this.hash = hash;
            this.data = data; this.ts = ts; this.nonce = nonce;
        }
    }

    public static final class Contract {
        public final String id, name, code;
        public int calls;
        public Contract(String id, String name, String code) {
            this.id = id; this.name = name; this.code = code;
        }
    }

    public static final class ForensicCase {
        public final String id, title;
        public final long created;
        public String status = "OPEN";
        public final List<String> evidence = new ArrayList<>();
        public final List<String> timeline = new ArrayList<>();
        public final List<String> chain = new ArrayList<>();
        public ForensicCase(String id, String title, long created) {
            this.id = id; this.title = title; this.created = created;
        }
    }

    public static final class GlobalEvent {
        public final String id, title, kind;
        public final long startsAt, endsAt;
        public final int xpReward;
        public GlobalEvent(String id, String title, String kind, long startsAt, long endsAt, int xpReward) {
            this.id = id; this.title = title; this.kind = kind;
            this.startsAt = startsAt; this.endsAt = endsAt; this.xpReward = xpReward;
        }
    }

    public static final class ArchComponent {
        public final String id, name, target, status;
        public final boolean interfaceBound;
        public ArchComponent(String id, String name, String target, String status, boolean interfaceBound) {
            this.id = id; this.name = name; this.target = target;
            this.status = status; this.interfaceBound = interfaceBound;
        }
    }
}
