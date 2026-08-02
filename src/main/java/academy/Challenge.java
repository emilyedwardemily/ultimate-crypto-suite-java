package academy;

/**
 * UC-FORTRESS ACADEMY - Immutable challenge model.
 *
 * <p>This is the canonical value object for challenges rendered inside the
 * Fortress Academy. It mirrors the legacy {@code Dashboard.ChallengeData}
 * shape so the two representations are freely interoperable while the new
 * feature set (Practice Lab, generated challenges, certificates) never has
 * to mutate the legacy 284-challenge array.</p>
 */
public final class Challenge {

    public final String id;
    public final String title;
    public final int stars;
    public final int xp;
    public final String diff;
    public final String family;
    public final String descr;
    public final String hint;
    public final String flag;

    public Challenge(String id, String title, int stars, int xp, String diff,
                     String family, String descr, String hint, String flag) {
        this.id = id;
        this.title = title;
        this.stars = stars;
        this.xp = xp;
        this.diff = diff;
        this.family = family;
        this.descr = descr;
        this.hint = hint;
        this.flag = flag;
    }

    @Override
    public String toString() {
        return "Challenge[" + id + "|" + title + "|" + diff + "|" + xp + "XP]";
    }
}
