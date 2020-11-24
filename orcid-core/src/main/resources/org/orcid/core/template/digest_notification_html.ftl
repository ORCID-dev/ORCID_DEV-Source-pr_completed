<#import "email_macros.ftl" as emailMacros />
<#escape x as x?html>
    <!DOCTYPE html>
    <html>
    <head>
        <title>${subject}</title>
    </head>
    <body>
    <div style="
                max-width: 736px;
                padding: 32px;
                margin: auto;
                font-family: Arial, helvetica, sans-serif;
                color: #494A4C;
                font-size: 15px;
            ">
        <#list digestEmail.notificationsBySourceId?keys?sort as sourceId>
            <#if sourceId != 'ORCID'>
                <#list digestEmail.notificationsBySourceId[sourceId].notificationsByType?keys?sort as notificationType>
                    <#include "notification_header_html.ftl"/>
                    <#if notificationType == 'PERMISSION' || notificationType == 'INSTITUTIONAL_CONNECTION'>
                        <hr style="color: #ff9c00;background-color: #ff9c00;border-style: solid;border-width: 2px;"/>
                        <div style="font-weight: bold;display: flex;align-items: center;text-align: start;">
                            <span style="background-color:#ff9c00;height: 8px;width: 8px;border-radius: 50%;display: inline-block;margin-right: 8px;margin-top: 10px;"></span>
                            <p style="color: #ff9c00;margin: 6px 0;font-size: 12px;font-weight: bold;"><@emailMacros.msg "notification.digest.permissions" /></p>
                        </div>
                        <p style="margin: 15px 0;font-weight: bold;">
                            <#list digestEmail.sources as source>
                                <#if source != 'ORCID'>
                                    ${source}
                                    <#if (digestEmail.sources?size gt 1)>
                                        ,
                                    </#if>
                                </#if>
                            </#list>
                            <@emailMacros.space /><@emailMacros.msg "notification.digest.askedPermission" /></p>
                        <hr style="color: #ff9c00;background-color: #ff9c00;border-style: solid;border-width: 2px;"/>
                    <#elseif notificationType == 'AMENDED'>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                        <div style="font-weight: bold;display: flex;align-items: center;text-align: start;">
                            <span style="background-color:#447405;height: 8px;width: 8px;border-radius: 50%;display: inline-block;margin-right: 8px;margin-top: 10px;"></span>
                            <p style="color: #447405;margin: 6px 0;font-size: 12px;font-weight: bold;"><@emailMacros.msg "notification.digest.record" /></p>
                        </div>
                        <p style="margin: 15px 0;font-weight: bold;">
                            <#list digestEmail.sources as source>
                                <#if source != 'ORCID'>
                                    ${source}
                                    <#if (digestEmail.sources?size gt 1)>
                                        ,
                                    </#if>
                                </#if>
                            </#list>
                            <@emailMacros.space /><@emailMacros.msg "notification.digest.hasChanges" /></p>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                    <#elseif notificationType == 'ADMINISTRATIVE'>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                        <div style="font-weight: bold;display: flex;align-items: center;text-align: start;">
                            <span style="background-color:#447405;height: 8px;width: 8px;border-radius: 50%;display: inline-block;margin-right: 8px;margin-top: 10px;"></span>
                            <p style="color: #447405;margin: 6px 0;font-size: 12px;font-weight: bold;"><@emailMacros.msg "notification.digest.record" /></p>
                        </div>
                        <p style="margin: 15px 0;font-weight: bold;">
                            <#list digestEmail.sources as source>
                                <#if source != 'ORCID'>
                                    ${source}
                                    <#if (digestEmail.sources?size gt 1)>
                                        ,
                                    </#if>
                                </#if>
                            </#list>
                            <@emailMacros.space /><@emailMacros.msg "notification.digest.hasChanges" /></p>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                    <#else>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                        <div style="font-weight: bold;display: flex;align-items: center;text-align: start;">
                            <span style="background-color:#447405;height: 8px;width: 8px;border-radius: 50%;display: inline-block;margin-right: 8px;margin-top: 10px;"></span>
                            <p style="color: #447405;margin: 6px 0;font-size: 12px;font-weight: bold;"><@emailMacros.msg "notification.digest.data" /></p>
                        </div>
                        <hr style="color: #447405;background-color: #447405;border-style: solid;border-width: 2px;"/>
                    </#if>
                    <#list digestEmail.notificationsBySourceId[sourceId].notificationsByType[notificationType] as notification>
                        <#if notificationType == 'PERMISSION'>
                            <p><#if notification.notificationSubject??>${notification.notificationSubject} <#if notification.createdDate??>(${notification.createdDate.year?c}-<#if notification.createdDate.month?string?length == 1>0${notification.createdDate.month?c}<#else>${notification.createdDate.month?c}</#if>-<#if notification.createdDate.day?string?length == 1>0${notification.createdDate.day?c}<#else>${notification.createdDate.day?c}</#if>)</#if><#else><@emailMacros.msg "email.digest.requesttoadd" /> <#if notification.createdDate??>(${notification.createdDate.year?c}-<#if notification.createdDate.month?string?length == 1>0${notification.createdDate.month?c}<#else>${notification.createdDate.month?c}</#if>-<#if notification.createdDate.day?string?length == 1>0${notification.createdDate.day?c}<#else>${notification.createdDate.day?c}</#if>)</#if></#if></p>
                            <br>
                            <#assign itemsByType=notification.items.itemsByType>
                            <#list itemsByType?keys?sort as itemType>
                                <b><@emailMacros.msg "email.common.recordsection." + itemType /></b> (${itemsByType[itemType]?size})
                                <br>
                                <#list itemsByType[itemType] as item>
                                    *<@emailMacros.space />${item.itemName?trim} <#if item.externalIdentifier??>(${item.externalIdentifier.type?lower_case}: ${item.externalIdentifier.value})</#if>
                                    <br>
                                </#list>
                            </#list>
                        <#elseif notificationType == 'AMENDED' && !verboseNotifications>
                            <p>
                                <@emailMacros.msg "notification.digest.showing" />
                                <@emailMacros.space /><b>${digestEmail.sources?size}</b><@emailMacros.space />
                                <@emailMacros.msg "notification.digest.outOf" /><@emailMacros.space />
                                <b>${digestEmail.sources?size}</b><@emailMacros.space />
                                <@emailMacros.msg "notification.digest.changes" />
                            </p>
                            <p><b>${(digestEmail.notificationsBySourceId[sourceId].source.sourceName.content)!sourceId}</b></p>
                            <#assign amendedSection><@emailMacros.msg "email.common.recordsection." + notification.amendedSection /></#assign>
                            <br>
                            <@emailMacros.msg "email.digest.hasupdated_1" />
                            <br>
                            ${(digestEmail.notificationsBySourceId[sourceId].source.sourceName.content)!sourceId}<@emailMacros.space /><@emailMacros.msg "email.digest.hasupdated_2" /><@emailMacros.space />${amendedSection?lower_case}<@emailMacros.space /><@emailMacros.msg "email.digest.hasupdated_3" /><@emailMacros.space /><#if notification.createdDate??>(${notification.createdDate.year?c}-<#if notification.createdDate.month?string?length == 1>0${notification.createdDate.month?c}<#else>${notification.createdDate.month?c}</#if>-<#if notification.createdDate.day?string?length == 1>0${notification.createdDate.day?c}<#else>${notification.createdDate.day?c}</#if>)</#if>
                        <#elseif notificationType == 'INSTITUTIONAL_CONNECTION'>
                            <@emailMacros.msg 'email.institutional_connection.1' /><@emailMacros.space />${(notification.idpName)!}<@emailMacros.space /><@emailMacros.msg 'email.institutional_connection.2' /><@emailMacros.msg 'email.institutional_connection.here' /><@emailMacros.msg 'email.institutional_connection.3' /><@emailMacros.space />${(notification.source.sourceName.content)!sourceId}<@emailMacros.space /><@emailMacros.msg 'email.institutional_connection.4' /><@emailMacros.space />${baseUri}/inbox/encrypted/${notification.encryptedPutCode}/action <#if notification.createdDate??>(${notification.createdDate.year?c}-<#if notification.createdDate.month?string?length == 1>0${notification.createdDate.month?c}<#else>${notification.createdDate.month?c}</#if>-<#if notification.createdDate.day?string?length == 1>0${notification.createdDate.day?c}<#else>${notification.createdDate.day?c}</#if>)</#if>
                            <br>
                        <#elseif notificationType != 'AMENDED'>
                            <#if notificationType == 'ADMINISTRATIVE'>
                                <#if subjectDelegate??>
                                    <#if subjectDelegate?ends_with("has made you an Account Delegate for their ORCID record")>
                                        <@bodyHtmlDelegateRecipient?interpret />
                                    <#elseif subjectDelegate?ends_with("has been added as a Trusted Individual")>
                                        <@bodyHtmlDelegate?interpret />
                                    <#elseif subjectDelegate?starts_with("[ORCID] Trusting")>
                                        <@bodyHtmlAdminDelegate?interpret />
                                    <#else>
                                        ${(notification.subject)}
                                    </#if>
                                </#if>
                            <#else>
                                ${(notification.subject)}
                            </#if>
                        </#if>
                    </#list>
                </#list>
            </#if>
        </#list>
        <br>
        <#if verboseNotifications>
            <#include "digest_email_amend_section.ftl"/>
        </#if>
        <br>
        <#include "notification_footer_html.ftl"/>
    </div>
    </body>
    </html>
</#escape>
