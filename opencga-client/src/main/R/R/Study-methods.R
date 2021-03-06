################################################################################
#' StudyClient methods
#' @include commons.R
#' 
#' @description This function implements the OpenCGA calls for managing Studies
#' @slot OpencgaR an object OpencgaR generated using initOpencgaR and/or opencgaLogin 
#' where the connection and session details are stored
#' @slot study a character string of the study to work on, in the format 
#' [[user@]project:]study where study and project can be either the id or alias. 
#' Mandatory when using studyClient with all actions except "create" and "search" 
#' and all actions in studyGroupClient
#' @slot group a character string containing the groupId to work with. Mandatory 
#' when using studyGroupClient "delete" action. If specified when using "update", 
#' updates the members of the group, otherwise, is used to add/remove users with 
#' access to study
#' @slot memberId a character or a vector contaning user or group ids to 
#' work on. Mandatory when using studyAclClient
#' @slot action action to be performed on the studies
#' @slot params list containing additional query or body params
#' @seealso \url{https://github.com/opencb/opencga/wiki} and the RESTful API documentation 
#' \url{http://bioinfo.hpc.cam.ac.uk/opencga/webservices/}
#' @export

setMethod("studyClient", "OpencgaR", function(OpencgaR, study, action, params=NULL, ...) {
    category <- "studies"
    switch(action,
           search=fetchOpenCGA(object=OpencgaR, category=category,  
                               action="search", params=params, httpMethod="GET", ...),
           acl=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                            action="acl", params=params, httpMethod="GET", ...),
           groups=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                               action="groups", params=params, httpMethod="GET", ...),
           info=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                             action="info", params=params, httpMethod="GET", ...),
           summary=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                action="summary", params=params, httpMethod="GET", ...),
           permissionRules=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                        action="permissionRules", params=params, httpMethod="GET", ...),
           stats=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study,
                                        action="stats", params=params, httpMethod="GET", ...),
           variableSets=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                     action="variableSets", params=params, httpMethod="GET", ...),
           update=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                               action="update", params=params, httpMethod="POST", ...),
           create=fetchOpenCGA(object=OpencgaR, category=category, action="create", 
                               params=params, httpMethod="POST", as.queryParam="project", ...)
           # delete=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                     action="delete", params=params, httpMethod="GET", ...),
           # files=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                    action="files", params=params, httpMethod="GET", ...),
           # jobs=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                   action="jobs", params=params, httpMethod="GET", ...),
           # resyncFiles=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                          action="resyncFiles", params=params, httpMethod="GET", ...),
           # samples=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                      action="samples", params=params, httpMethod="GET", ...),
           # scanFiles=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
           #                        action="scanFiles", params=params, httpMethod="GET", ...)
           
    )
})

#' @export
setMethod("studyGroupClient", "OpencgaR", function(OpencgaR, study, group=NULL, 
                                                   action, params=NULL, ...) {
    category <- "studies"
    switch(action,
           # delete=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study,
           #                     subcategory="groups", subcategoryId=group, 
           #                     action=action, params=params, httpMethod="GET", ...),
           create=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                               subcategory="groups", action=action, params=params, 
                               as.queryParam="action", httpMethod="POST", ...),
           update={
               if (is.null(group)) {
                   fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                subcategory="groups", action=action, 
                                params=params, httpMethod="POST", ...)
               }else{
                   fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                subcategory="groups", subcategoryId=group, 
                                action="/users/update", params=params, as.queryParam="action",
                                httpMethod="POST", ...)
               }}
    )
})

#' @export
setMethod("studyAclClient", "OpencgaR", function(OpencgaR, study, memberId, action, params=NULL, ...) {
    category <- "studies"
    switch(action,
           update=fetchOpenCGA(object=OpencgaR, category=category, 
                               subcategory="acl", subcategoryId=memberId, 
                               action=action, params=params, httpMethod="POST", ...)
    )
})

#' @export
setMethod("studyVariablesetClient", "OpencgaR", function(OpencgaR, study, variableSet=NULL, action, params=NULL, ...) {
    category <- "studies"
    switch(action,
           # search=fetchOpenCGA(object=OpencgaR, category=category, 
           #                     action=action, params=params, httpMethod="GET", ...),
           # delete=fetchOpenCGA(object=OpencgaR, category=category, 
           #                     categoryId=variableSet, action=action, 
           #                     params=params, httpMethod="GET", ...),
           # info=fetchOpenCGA(object=OpencgaR, category=category, 
           #                   categoryId=variableSet, action=action, 
           #                   params=params, httpMethod="GET", ...),
           # summary=fetchOpenCGA(object=OpencgaR, category=category, 
           #                      categoryId=variableSet, action=action, 
           #                      params=params, httpMethod="GET", ...),
           # create=fetchOpenCGA(object=OpencgaR, category=category, 
           #                     action=action, params=params, httpMethod="POST", ...),
           update={
               if (is.null(variableSet)) {
                   fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                subcategory="variableSets", action=action, 
                                params=params, as.queryParam="action", httpMethod="POST", ...)
               }else{
                   fetchOpenCGA(object=OpencgaR, category=category, categoryId=study, 
                                subcategory="variableSets", subcategoryId=variableSet, 
                                action="/variables/update", params=params, as.queryParam="action",
                                httpMethod="POST", ...)
               }}
    )
})

#' @export
setMethod("studyPermissionRulesClient", "OpencgaR", function(OpencgaR, study, action, params=NULL, ...) {
    category <- "studies"
    switch(action,
           update=fetchOpenCGA(object=OpencgaR, category=category, categoryId=study,
                               subcategory="permissionRules", action=action, 
                               params=params, as.queryParam=c("action", "entity"),
                               httpMethod="POST", ...)
    )
})


#' setMethod("studyVariablesetFieldClient", "OpencgaR", function(OpencgaR, variableSet, action, params=NULL, ...) {
#'     category <- "studies"
#'     switch(action,
#'            delete=fetchOpenCGA(object=OpencgaR, category=category, 
#'                                categoryId=variableSet, subcategory="field", 
#'                                action=action, params=params, httpMethod="GET", ...),
#'            rename=fetchOpenCGA(object=OpencgaR, category=category, 
#'                                categoryId=variableSet, subcategory="field", 
#'                                action=action, params=params, httpMethod="GET", ...),
#'            add=fetchOpenCGA(object=OpencgaR, category=category, 
#'                             categoryId=variableSet, action=action, 
#'                             params=params, httpMethod="POST", ...)
#'     )
#' })