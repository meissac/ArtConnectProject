package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of CommunityMemberDao.
 */
public class JdbcCommunityMemberDao implements CommunityMemberDao {

    @Override
    public Optional<CommunityMember> findById(Long id) {
        String sql = """
                SELECT cm.member_id, cm.name, cm.email,
                       cm.birth_year, cm.phone, cm.city,
                       cm.membership_type,
                       d.name AS discipline_name
                FROM community_member cm
                LEFT JOIN member_discipline md
                    ON cm.member_id = md.member_id
                LEFT JOIN discipline d
                    ON md.discipline_id = d.discipline_id
                WHERE cm.member_id = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            CommunityMember member = null;
            while (rs.next()) {
                if (member == null) {
                    member = mapRowToMember(rs);
                }
                String disciplineName = rs.getString("discipline_name");
                if (disciplineName != null) {
                    member.getFavoriteDisciplines()
                            .add(new Discipline(disciplineName));
                }
            }
            return Optional.ofNullable(member);

        } catch (SQLException e) {
            System.err.println("Error fetching member by id: "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<CommunityMember> findAll() {
        String sql = """
                SELECT cm.member_id, cm.name, cm.email,
                       cm.birth_year, cm.phone, cm.city,
                       cm.membership_type,
                       d.name AS discipline_name
                FROM community_member cm
                LEFT JOIN member_discipline md
                    ON cm.member_id = md.member_id
                LEFT JOIN discipline d
                    ON md.discipline_id = d.discipline_id
                ORDER BY cm.member_id
                """;

        Map<Integer, CommunityMember> memberMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int memberId = rs.getInt("member_id");

                if (!memberMap.containsKey(memberId)) {
                    memberMap.put(memberId, mapRowToMember(rs));
                }

                String disciplineName = rs.getString("discipline_name");
                if (disciplineName != null) {
                    memberMap.get(memberId)
                            .getFavoriteDisciplines()
                            .add(new Discipline(disciplineName));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching members: " + e.getMessage());
        }

        return new ArrayList<>(memberMap.values());
    }

    private CommunityMember mapRowToMember(ResultSet rs) throws SQLException {
        CommunityMember member = new CommunityMember();
        member.setName(rs.getString("name"));
        member.setEmail(rs.getString("email"));
        member.setBirthYear(rs.getInt("birth_year"));
        member.setPhone(rs.getString("phone"));
        member.setCity(rs.getString("city"));
        member.setMembershipType(rs.getString("membership_type"));
        return member;
    }
}