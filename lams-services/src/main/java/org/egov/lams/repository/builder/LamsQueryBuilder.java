package org.egov.lams.repository.builder;

import java.util.Arrays;
import java.util.List;

import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LamsQueryBuilder {

    private LamsConfiguration config;

    @Autowired
    public LamsQueryBuilder(LamsConfiguration config) {
        this.config = config;
    }

    private static final String INNER_JOIN_STRING = " INNER JOIN ";
    
    private static final String LEFT_JOIN = " LEFT JOIN ";

    private static final String QUERY = "SELECT renewal.*,renewaldetail.*,renewal.id as renewal_id,renewal.tenantid as renewal_tenantId,"
    		+ "renewal.lastModifiedTime as renewal_lastModifiedTime,renewal.createdBy as renewal_createdBy,"
    		+ "renewal.lastModifiedBy as renewal_lastModifiedBy,renewal.createdTime as renewal_createdTime,"
    		+ "renewaldetail.id as renewaldetail_id,renewaldetail.lastModifiedTime as renewaldetail_lastModifiedTime,"
    		+ "renewaldetail.createdBy as renewaldetail_createdBy,renewaldetail.lastModifiedBy as renewaldetail_lastModifiedBy,"
    		+ "renewaldetail.createdTime as renewaldetail_createdTime,renewaldetail.surveyno as renewal_surveyno,"
    		+ "renewal.accountId as uuid  FROM eg_lams_leaserenewal renewal "
    		+ LEFT_JOIN
    		+ "eg_lams_leaserenewaldetail renewaldetail ON renewaldetail.leaserenewalid = renewal.id "
    		+ INNER_JOIN_STRING 
    		+ "eg_lams_survey_no_details surveydetail ON surveydetail.surveyno = renewaldetail.surveyno";


      private final String paginationWrapper = "SELECT * FROM " +
              "(SELECT *, DENSE_RANK() OVER (ORDER BY renewal_lastModifiedTime DESC , renewal_id) offset_ FROM " +
              "({})" +
              " result) result_offset " +
              "WHERE offset_ > ? AND offset_ <= ?";


    public String getLeaseRenewalSearchQuery(SearchCriteria criteria, List<Object> preparedStmtList) {

        StringBuilder builder = new StringBuilder(QUERY);

        addBusinessServiceClause(criteria,preparedStmtList,builder);


        if(criteria.getAccountId()!=null){
            addClauseIfRequired(preparedStmtList,builder);
            builder.append(" lr.accountid = ? ");
            preparedStmtList.add(criteria.getAccountId());

        }
        else {

            if (criteria.getTenantId() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" lr.tenantid=? ");
                preparedStmtList.add(criteria.getTenantId());
            }
            List<String> ids = criteria.getIds();
            if (!CollectionUtils.isEmpty(ids)) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" lr.id IN (").append(createQuery(ids)).append(")");
                addToPreparedStatement(preparedStmtList, ids);
            }

            if (criteria.getApplicationNumber()!= null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  lr.applicationnumber = ? ");
                preparedStmtList.add(criteria.getApplicationNumber());
            }
            if (criteria.getStatus() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  lr.status = ? ");
                preparedStmtList.add(criteria.getStatus());
            }
        }
        return addPaginationWrapper(builder.toString(),preparedStmtList,criteria);
    }


    private void addBusinessServiceClause(SearchCriteria criteria,List<Object> preparedStmtList,StringBuilder builder){
    	if(criteria.getBusinessService()!=null) {
    	List<String> businessServices = Arrays.asList(criteria.getBusinessService().split(","));
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" lr.businessservice IN (").append(createQuery(businessServices)).append(")");
            addToPreparedStatement(preparedStmtList, businessServices);
    }
    }

    private String createQuery(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        int length = ids.size();
        for( int i = 0; i< length; i++){
            builder.append(" ?");
            if(i != length -1) builder.append(",");
        }
        return builder.toString();
    }

    private void addToPreparedStatement(List<Object> preparedStmtList,List<String> ids)
    {
        ids.forEach(id ->{ preparedStmtList.add(id);});
    }


    private String addPaginationWrapper(String query,List<Object> preparedStmtList,
                                      SearchCriteria criteria){
        int limit = config.getDefaulLamsimit();
        int offset = config.getDefaultOffset();
        String finalQuery = paginationWrapper.replace("{}",query);

        if(criteria.getLimit()!=null && criteria.getLimit()<=config.getMaxSearchLimit())
            limit = criteria.getLimit();

        if(criteria.getLimit()!=null && criteria.getLimit()>config.getMaxSearchLimit())
            limit = config.getMaxSearchLimit();

        if(criteria.getOffset()!=null)
            offset = criteria.getOffset();

        preparedStmtList.add(offset);
        preparedStmtList.add(limit+offset);

       return finalQuery;
    }


    private static void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");
        else {
            queryString.append(" AND");
        }
    }





}
