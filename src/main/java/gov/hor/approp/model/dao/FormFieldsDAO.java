package gov.hor.approp.model.dao;

import gov.hor.approp.model.Agency;
import gov.hor.approp.model.Award;
import gov.hor.approp.model.Bureau;
import gov.hor.approp.model.Congress;
import gov.hor.approp.model.csv.GrantView;
import gov.hor.approp.model.Member;
import gov.hor.approp.model.Program;
import gov.hor.approp.model.State;
import gov.hor.approp.model.Subcommittee;
import gov.hor.approp.model.csv.Grant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

@Repository
public class FormFieldsDAO {

    @PersistenceContext
    private EntityManager em;

    public List<Agency> getAgencies(List<String> subcommittee) {
        String query = "select distinct on(agency_name) agency_code, agency_name from programs.cfda_account WHERE (subcommittee in (:subcommittee)) or (:subfirst='unspecified') order by agency_name";
        Query q = em.createNativeQuery(query, Agency.class);
        q.setParameter("subcommittee", subcommittee);
        String subfirst = isOnlyUnspecified(subcommittee);
        q.setParameter("subfirst", subfirst);
        List<Agency> agencies = q.getResultList();
        Agency unspec = new Agency();
        unspec.setAgency_code("unspecified");
        unspec.setAgency_name("unspecified");
        agencies.add(0, unspec);
        return agencies;
    }

    private String isOnlyUnspecified(List<String> subcommittee) {
        String out = "userchoice";
        if (subcommittee.size() == 1 && subcommittee.get(0).equals("unspecified")) {
            out = "unspecified";
        }
        return out;
    }

    public List<Bureau> getBureaus(List<String> agency, List<String> subcommittee) {
        String query = "select distinct on(bureau_name) bureau_code, bureau_name from programs.cfda_account where bureau_name is not null "
                + "AND (agency_name in (:agency) or :agencyfirst='unspecified') AND (subcommittee in (:subcommittee) or :subfirst='unspecified') order by bureau_name";
        Query q = em.createNativeQuery(query, Bureau.class);
        q.setParameter("agency", agency);
        String agencyfirst = isOnlyUnspecified(agency);
        q.setParameter("agencyfirst", agencyfirst);
        q.setParameter("subcommittee", subcommittee);
        String subfirst = isOnlyUnspecified(subcommittee);
        q.setParameter("subfirst", subfirst);
        List<Bureau> bureaus = q.getResultList();
        Bureau unspecified = new Bureau();
        unspecified.setBureau_code("unspecified");
        unspecified.setBureau_name("unspecified");
        bureaus.add(0, unspecified);
        return bureaus;
    }

    private List<String> commaListToList(String subcommittee) {
        return Arrays.asList(subcommittee.split(","));
    }

    public List<Subcommittee> getSubcommittees() {
        String query = "select distinct subcommittee from programs.cfda_account where subcommittee is not null order by subcommittee";
        Query q = em.createNativeQuery(query, Subcommittee.class);
        List<Subcommittee> subcommittees = q.getResultList();
        Subcommittee unspecified = new Subcommittee();
        unspecified.setSubcommittee("unspecified");
        subcommittees.add(0, unspecified);
        return subcommittees;
    }

    public List<Program> getPrograms(List<String> bureau, List<String> subcommittee, List<String> agency) {
        String query = "select cfda, program_title from programs.cfda_account "
                + "where (bureau_name in (:bureau) or :bureaufirst='unspecified') "
                + "AND (agency_name in (:agency) or :agencyfirst='unspecified') "
                + "AND (subcommittee in (:subcommittee) or :subfirst='unspecified') "
                + "order by cfda";
        Query q = em.createNativeQuery(query, Program.class);
        q.setParameter("bureau", bureau);
        String bureaufirst = isOnlyUnspecified(bureau);
        q.setParameter("bureaufirst", bureaufirst);
        q.setParameter("agency", agency);
        String agencyfirst = isOnlyUnspecified(agency);
        q.setParameter("agencyfirst", agencyfirst);
        q.setParameter("subcommittee", subcommittee);
        String subfirst = isOnlyUnspecified(subcommittee);
        q.setParameter("subfirst", subfirst);
        return q.getResultList();
    }

    public List<State> getStates() {
        String query = "select name, stusps, st_envelope(the_geom) as the_geom from tiger.state order by name";
        Query q = em.createNativeQuery(query, State.class);
        List<State> states = q.getResultList();
        State unspecified = new State();
        unspecified.setName("unspecified");
        unspecified.setAbbrev("unspecified");
        states.add(0, unspecified);
        return states;
    }

    public List<Member> getMemberAutoComplete(String partial, Integer congress, String state) {
        String query = "select distinct on(lastname,firstname,middlename) lastname, firstname, middlename, m.id as id, st_envelope(c.the_geom) as the_geom "
                + "from members.member m, members.term t, tiger.cd c, tiger.state s "
                + "where m.id=t.memberid and c.cdfp=t.cdfp AND c.statefp=t.statefp AND s.statefp=t.statefp AND t.congress=:congress AND upper(lastname) like (:partial) "
                + "and (s.name=:state or 'unspecified'=:state)";
        Query q = em.createNativeQuery(query, Member.class);
        q.setParameter("partial", "%" + partial.toUpperCase() + "%");
        q.setParameter("congress", congress);
        q.setParameter("state", state);
        return q.getResultList();
    }

    public List<Congress> getCongresses() {
        String query = "select distinct congress from members.term order by congress desc";
        Query q = em.createNativeQuery(query, Congress.class);
        return q.getResultList();
    }

    public List<Program> getCfdaAutoComplete(String partial) {
        String query = "select cfda, program_title from programs.cfda_account where upper(program_title) like :partial order by program_title";
        Query q = em.createNativeQuery(query, Program.class);
        q.setParameter("partial", "%" + partial.toUpperCase() + "%");
        return q.getResultList();
    }

    public List<String> getYears() {
        String query = "select distinct fiscal_year from spending.grant_geocoded_usaspending7 order by fiscal_year desc";
        Query q = em.createNativeQuery(query);
        return q.getResultList();
    }

    public List<Award> getAwards(List<String> fy) {
        /*    	String query = "select gg.gid, "
         + "gg.award_date, "
         + "gg.award_amount, "
         + "gg.fiscal_year,"
         + "'Grant' as grant_or_subgrant, "
         + "g.assistance_type, "
         + "gg.cfda, "
         + "ca.subcommittee, "
         + "ca.program_title, "
         + "ca.agency_name, "
         + "ca.bureau_name, "
         + "gg.recipient_name, "
         + "gg.address, "
         + "gg.city, "
         + "gg.state, "
         + "gg.zip, "
         + "gg.zip4, "
         + "gg.state || gg.cdfp as cd_at_award, "
         + "m_hist.firstname || ' ' || coalesce(m_hist.middlename || ' ', '') || m_hist.lastname as member_at_award, "
         + "t_hist.party as party_at_award, gg.state || gg.cd_currentcdfp as cd_current, gg.member_currentid, m_current.firstname || ' ' || coalesce(m_current.middlename || ' ', '') || m_current.lastname as member_current, "
         + "t_current.party as party_current, "
         + "g.recipient_cd as usa_spending_cd "
         + "from spending.grant_geocoded_usaspending3 as gg "
         + "left join spending.grant as g on gg.gid = g.id "
         + "left join programs.cfda_account as ca on gg.cfda = ca.cfda "
         + "left join members.member as m_hist on gg.memberid = m_hist.id "
         + "left join members.member as m_current on gg.member_currentid = m_current.id "
         + "left join members.term as t_hist on gg.termid = t_hist.id "
         + "left join members.term as t_current on gg.term_currentid = t_current.id "
         + "where gg.fiscal_year in (:fy)";*/
        String query = "select * from spending.award2 where fiscal_year in (:fy)";
        Query q = em.createNativeQuery(query, Award.class);
        q.setParameter("fy", fy);
        return q.getResultList();
    }

    public List<GrantView> getGrantViews(List<String> fiscal_year,
            List<String> subcommittee,
            List<String> agency_name,
            List<String> bureau_name,
            List<String> cfda,
            List<String> state) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<GrantView> query = builder.createQuery(GrantView.class);
        // TODO: Generate metamodel classes so these fields aren't hardcoded.
//        Metamodel m = em.getMetamodel();
//        EntityType<GrantView> GrantView_ = m.entity(GrantView.class);
        Root<GrantView> grantView = query.from(GrantView.class);
        // Use fetch to join the tables in the same request. Otherwise JPA will make separate requests for each row.
        grantView.fetch("awardMember");
        grantView.fetch("awardTerm");
        grantView.fetch("currentMember");
        grantView.fetch("currentTerm");

        ArrayList<Predicate> predicates = new ArrayList<>();

        if (includeList(fiscal_year)) {
            predicates.add(grantView.get("fiscal_year").in(fiscal_year));
        }

        if (includeList(subcommittee)) {
            predicates.add(grantView.get("subcommittee").in(subcommittee));
        }

        if (includeList(agency_name)) {
            predicates.add(grantView.get("agency_name").in(agency_name));
        }

        if (includeList(bureau_name)) {
            predicates.add(grantView.get("bureau_name").in(bureau_name));
        }

        if (includeList(cfda)) {
            predicates.add(grantView.get("cfda").in(cfda));
        }

        if (includeList(state)) {
            predicates.add(grantView.get("state").in(state));
        }

        Predicate[] ps = predicates.toArray(new Predicate[predicates.size()]);

        query.where(builder.and(ps));

        return em.createQuery(query).getResultList();
    }

    public List<Grant> getGrants(List<String> fiscal_year,
            List<String> subcommittee,
            List<String> agency_name,
            List<String> bureau_name,
            List<String> cfda,
            List<String> state) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Grant> query = builder.createQuery(Grant.class);
        // TODO: Generate metamodel classes so these fields aren't hardcoded.
//        Metamodel m = em.getMetamodel();
//        EntityType<GrantView> GrantView_ = m.entity(GrantView.class);
        Root<Grant> grant = query.from(Grant.class);
        // Use fetch to join the tables in the same request. Otherwise JPA will make separate requests for each row.
        grant.fetch("grantView");

        ArrayList<Predicate> predicates = new ArrayList<>();

        if (includeList(fiscal_year)) {
            predicates.add(grant.get("grantView").<String>get("fiscal_year").in(fiscal_year));
        }

        if (includeList(subcommittee)) {
            predicates.add(grant.get("grantView").<String>get("subcommittee").in(subcommittee));
        }

        if (includeList(agency_name)) {
            predicates.add(grant.get("grantView").<String>get("agency_name").in(agency_name));
        }

        if (includeList(bureau_name)) {
            predicates.add(grant.get("grantView").<String>get("bureau_name").in(bureau_name));
        }

        if (includeList(cfda)) {
            predicates.add(grant.get("grantView").<String>get("cfda").in(cfda));
        }

        if (includeList(state)) {
            predicates.add(grant.get("grantView").<String>get("state").in(state));
        }

        Predicate[] ps = predicates.toArray(new Predicate[predicates.size()]);

        query.where(builder.and(ps));

        return em.createQuery(query).getResultList();
    }

    public boolean includeList(List<String> list) {
        return list != null && list.size() > 0 && !(list.size() == 1 && list.get(0).equals("unspecified"));
    }

}
