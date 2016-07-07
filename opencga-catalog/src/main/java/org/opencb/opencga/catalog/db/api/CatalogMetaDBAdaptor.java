package org.opencb.opencga.catalog.db.api;

/**
 * Created by pfurio on 23/05/16.
 */

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.models.Session;
import org.opencb.opencga.catalog.models.acls.StudyAcl;

import java.util.List;

public interface CatalogMetaDBAdaptor {

    boolean isRegisterOpen();

    QueryResult<ObjectMap> addAdminSession(Session session) throws CatalogDBException;

    String getAdminPassword() throws CatalogDBException;

    boolean checkValidAdminSession(String id);

    QueryResult<StudyAcl> getDaemonAcl(List<String> members) throws CatalogDBException;

}
