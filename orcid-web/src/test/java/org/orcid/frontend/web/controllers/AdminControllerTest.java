package org.orcid.frontend.web.controllers;

/**
 * @author Angel Montenegro (amontenegro) Date: 29/08/2013
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.core.admin.LockReason;
import org.orcid.core.common.manager.EmailFrequencyManager;
import org.orcid.core.exception.ClientAlreadyActiveException;
import org.orcid.core.exception.ClientAlreadyDeactivatedException;
import org.orcid.core.locale.LocaleManager;
import org.orcid.core.manager.AdminManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.TwoFactorAuthenticationManager;
import org.orcid.core.manager.v3.ClientDetailsManager;
import org.orcid.core.manager.v3.EmailManager;
import org.orcid.core.manager.v3.NotificationManager;
import org.orcid.core.manager.v3.OrcidSecurityManager;
import org.orcid.core.manager.v3.ProfileEntityManager;
import org.orcid.core.manager.v3.ProfileHistoryEventManager;
import org.orcid.core.manager.v3.SpamManager;
import org.orcid.core.manager.v3.impl.ProfileHistoryEventManagerImpl;
import org.orcid.core.manager.v3.read_only.EmailManagerReadOnly;
import org.orcid.core.oauth.OrcidProfileUserDetails;
import org.orcid.core.profile.history.ProfileHistoryEventType;
import org.orcid.core.security.OrcidUserDetailsService;
import org.orcid.core.security.OrcidWebRole;
import org.orcid.frontend.web.util.BaseControllerTest;
import org.orcid.jaxb.model.v3.release.common.Visibility;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.persistence.aop.ProfileLastModifiedAspect;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.dao.RecordNameDao;
import org.orcid.persistence.jpa.entities.EmailEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.persistence.jpa.entities.RecordNameEntity;
import org.orcid.pojo.AdminChangePassword;
import org.orcid.pojo.AdminDelegatesRequest;
import org.orcid.pojo.ClientActivationRequest;
import org.orcid.pojo.LockAccounts;
import org.orcid.pojo.ProfileDeprecationRequest;
import org.orcid.pojo.ProfileDetails;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.test.OrcidJUnit4ClassRunner;
import org.orcid.test.TargetProxyHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(OrcidJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:statistics-core-context.xml", "classpath:orcid-core-context.xml", "classpath:orcid-frontend-web-servlet.xml" })
public class AdminControllerTest extends BaseControllerTest {

    @Resource(name = "adminController")
    AdminController adminController;

    @Resource
    private ProfileDao profileDao;

    @Resource(name = "emailManagerV3")
    private EmailManager emailManager;

    @Resource(name = "notificationManagerV3")
    private NotificationManager notificationManager;
    
    @Resource
    private OrcidUserDetailsService orcidUserDetailsService;
    
    @Resource(name = "profileEntityManagerV3")
    private ProfileEntityManager profileEntityManager;
    
    @Mock
    private NotificationManager mockNotificationManager;
    
    @Mock
    private ProfileLastModifiedAspect mockProfileLastModifiedAspect; 
    
    @Mock
    private EmailManagerReadOnly mockEmailManagerReadOnly;
    
    @Mock
    private ProfileHistoryEventManager profileHistoryEventManager;
    
    @Mock
    private EmailFrequencyManager mockEmailFrequencyManager;
    
    @Mock
    private OrcidSecurityManager mockOrcidSecurityManager;
    
    @Mock
    private ClientDetailsManager clientDetailsManager;
    
    @Resource
    private EmailFrequencyManager emailFrequencyManager;
    
    @Resource
    private RecordNameDao recordNameDao;
    
    @Mock
    private TwoFactorAuthenticationManager mockTwoFactorAuthenticationManager;
    
    @Resource
    TwoFactorAuthenticationManager twoFactorAuthenticationManager;
    
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        initDBUnitData(Arrays.asList("/data/SourceClientDetailsEntityData.xml", "/data/ProfileEntityData.xml",
                "/data/RecordNameEntityData.xml", "/data/BiographyEntityData.xml", "/data/ClientDetailsEntityData.xml"));
    }

    @Before
    public void beforeInstance() {
        MockitoAnnotations.initMocks(this);
        Map<String, String> map = new HashMap<String, String>();
        map.put(EmailFrequencyManager.ADMINISTRATIVE_CHANGE_NOTIFICATIONS, String.valueOf(Float.MAX_VALUE));
        map.put(EmailFrequencyManager.CHANGE_NOTIFICATIONS, String.valueOf(Float.MAX_VALUE));
        map.put(EmailFrequencyManager.MEMBER_UPDATE_REQUESTS, String.valueOf(Float.MAX_VALUE));
        map.put(EmailFrequencyManager.QUARTERLY_TIPS, String.valueOf(true));

        when(mockEmailFrequencyManager.getEmailFrequency(anyString())).thenReturn(map);

        SecurityContextHolder.getContext().setAuthentication(getAuthentication());
        assertNotNull(adminController);
        assertNotNull(profileDao);

        TargetProxyHelper.injectIntoProxy(adminController, "orcidSecurityManager", mockOrcidSecurityManager);
        when(mockOrcidSecurityManager.isAdmin()).thenReturn(true);
        
        TargetProxyHelper.injectIntoProxy(profileEntityManager, "twoFactorAuthenticationManager", mockTwoFactorAuthenticationManager);
        doNothing().when(mockTwoFactorAuthenticationManager).disable2FA(Mockito.anyString());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        removeDBUnitData(Arrays.asList("/data/ClientDetailsEntityData.xml", "/data/RecordNameEntityData.xml", "/data/BiographyEntityData.xml",
                "/data/ProfileEntityData.xml", "/data/SourceClientDetailsEntityData.xml"));
    }

    @Override
    protected Authentication getAuthentication() {
        String orcid = "4444-4444-4444-4440";
        ProfileEntity p = profileEntityManager.findByOrcid(orcid);
        Email e = emailManager.findPrimaryEmail(orcid);
        List<OrcidWebRole> roles = getRole();
        OrcidProfileUserDetails details = new OrcidProfileUserDetails(orcid,
                e.getEmail(), null, roles);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(orcid, p.getPassword(), getRole());
        auth.setDetails(details);
        return auth;
    }

    protected List<OrcidWebRole> getRole() {
        return Arrays.asList(OrcidWebRole.ROLE_ADMIN);
    }

    @Test
    public void testCheckOrcid() throws Exception {
        ProfileDeprecationRequest r = new ProfileDeprecationRequest();
        ProfileDetails toDeprecate = new ProfileDetails();
        toDeprecate.setOrcid("4444-4444-4444-4447");
        r.setDeprecatedAccount(toDeprecate);
        ProfileDetails primary = new ProfileDetails();
        primary.setOrcid("4444-4444-4444-4411");
        r.setPrimaryAccount(primary);
        
        r = adminController.checkOrcidToDeprecate(mockRequest, mockResponse, r);
        assertNotNull(r);
        assertEquals(1, r.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.inexisting_orcid", "4444-4444-4444-4411"), r.getErrors().get(0));
        assertEquals("otis@reading.com", r.getDeprecatedAccount().getEmail());
        assertEquals("Family Name", r.getDeprecatedAccount().getFamilyName());
        assertEquals("Given Names", r.getDeprecatedAccount().getGivenNames());
        assertEquals("4444-4444-4444-4447", r.getDeprecatedAccount().getOrcid());
        
        assertEquals("4444-4444-4444-4411", r.getPrimaryAccount().getOrcid());
        assertNull(r.getPrimaryAccount().getEmail());
        assertNull(r.getPrimaryAccount().getFamilyName());
        assertNull(r.getPrimaryAccount().getGivenNames());        

        toDeprecate.setOrcid("https://orcid.org/4444-4444-4444-4447");
        r = adminController.checkOrcidToDeprecate(mockRequest, mockResponse, r);
        assertNotNull(r);
        assertEquals(1, r.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.inexisting_orcid", "4444-4444-4444-4411"), r.getErrors().get(0));
        assertEquals("otis@reading.com", r.getDeprecatedAccount().getEmail());
        assertEquals("Family Name", r.getDeprecatedAccount().getFamilyName());
        assertEquals("Given Names", r.getDeprecatedAccount().getGivenNames());
        assertEquals("4444-4444-4444-4447", r.getDeprecatedAccount().getOrcid());
        
        assertEquals("4444-4444-4444-4411", r.getPrimaryAccount().getOrcid());
        assertNull(r.getPrimaryAccount().getEmail());
        assertNull(r.getPrimaryAccount().getFamilyName());
        assertNull(r.getPrimaryAccount().getGivenNames());
    }

    @Test
    public void tryToDeprecateDeprecatedProfile() throws Exception {
        ProfileDeprecationRequest r = new ProfileDeprecationRequest();
        ProfileDetails toDeprecate = new ProfileDetails();
        toDeprecate.setOrcid("4444-4444-4444-444X");
        r.setDeprecatedAccount(toDeprecate);
        ProfileDetails primary = new ProfileDetails();
        primary.setOrcid("4444-4444-4444-4443");
        r.setPrimaryAccount(primary);
        
        // Test deprecating a deprecated account
        ProfileDeprecationRequest result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.already_deprecated", "4444-4444-4444-444X"), result.getErrors().get(0));

        // Test deprecating account with himself
        toDeprecate.setOrcid("4444-4444-4444-4440");
        primary.setOrcid("4444-4444-4444-4440");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.deprecated_equals_primary"), result.getErrors().get(0));

        // Test set deprecated account as a primary account
        toDeprecate.setOrcid("4444-4444-4444-4443");
        primary.setOrcid("4444-4444-4444-444X");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.primary_account_deprecated", "4444-4444-4444-444X"), result.getErrors().get(0));

        // Test deprecating an invalid orcid
        toDeprecate.setOrcid("4444-4444-4444-444");
        primary.setOrcid("4444-4444-4444-4443");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.invalid_orcid", "4444-4444-4444-444"), result.getErrors().get(0));

        // Test use invalid orcid as primary
        toDeprecate.setOrcid("4444-4444-4444-4440");
        primary.setOrcid("4444-4444-4444-444");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.invalid_orcid", "4444-4444-4444-444"), result.getErrors().get(0));

        // Deactivate primary record
        adminController.deactivateOrcidRecords(mockRequest, mockResponse, "4444-4444-4444-4443");
        
        // Test set deactive primary account
        toDeprecate.setOrcid("4444-4444-4444-4440");
        primary.setOrcid("4444-4444-4444-4443");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.primary_account_is_deactivated", "4444-4444-4444-4443"), result.getErrors().get(0));
        
        // Deactivate primary record
        adminController.deactivateOrcidRecords(mockRequest, mockResponse, "4444-4444-4444-4443");
        
        // Test set deactive primary account with ORCURL 
        toDeprecate.setOrcid("https://orcid.org/4444-4444-4444-4440");
        primary.setOrcid("4444-4444-4444-4443");
        result = adminController.deprecateProfile(mockRequest, mockResponse, r);
        assertEquals(1, result.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_deprecation.errors.primary_account_is_deactivated", "4444-4444-4444-4443"), result.getErrors().get(0));
    }

    @Test
    public void deactivateAndReactivateProfileTest() throws Exception {
        ProfileHistoryEventManager profileHistoryEventManager = Mockito.mock(ProfileHistoryEventManagerImpl.class);
        ProfileEntityManager profileEntityManager = (ProfileEntityManager) ReflectionTestUtils.getField(adminController, "profileEntityManager");
        ReflectionTestUtils.setField(profileEntityManager, "profileHistoryEventManager", profileHistoryEventManager);
        Mockito.doNothing().when(profileHistoryEventManager).recordEvent(Mockito.any(ProfileHistoryEventType.class), Mockito.anyString(), Mockito.anyString());
        
        // Test deactivate
        Map<String, Set<String>> result = adminController.deactivateOrcidRecords(mockRequest, mockResponse, "4444-4444-4444-4445");
        assertEquals(1, result.get("success").size());

        ProfileEntity deactivated = profileDao.find("4444-4444-4444-4445");
        assertNotNull(deactivated.getDeactivationDate());
        RecordNameEntity name = recordNameDao.getRecordName("4444-4444-4444-4445", System.currentTimeMillis());
        assertEquals("Family Name Deactivated", name.getFamilyName());
        assertEquals("Given Names Deactivated", name.getGivenNames());

        // Test try to deactivate an already deactive account
        result = adminController.deactivateOrcidRecords(mockRequest, mockResponse, "4444-4444-4444-4445");
        assertEquals(1, result.get("alreadyDeactivated").size());

        // Test reactivate using an email address that belongs to other record
        ProfileDetails proDetails = new ProfileDetails();
        proDetails.setEmail("public_0000-0000-0000-0003@test.orcid.org");
        proDetails.setOrcid("4444-4444-4444-4445");
        proDetails = adminController.reactivateOrcidRecord(mockRequest, mockResponse, proDetails);
        assertEquals(1, proDetails.getErrors().size());
        assertEquals(adminController.getMessage("admin.errors.deactivated_account.orcid_id_dont_match", "0000-0000-0000-0003"), proDetails.getErrors().get(0));
        
        // Test reactivate using empty primary email
        proDetails.setEmail("");
        try {
            proDetails = adminController.reactivateOrcidRecord(mockRequest, mockResponse, proDetails);
            fail();
        } catch(RuntimeException re) {
            assertEquals("Unable to filter empty email address", re.getMessage());
        } catch(Exception e) {
            fail();
        }
        
        // Test reactivate
        proDetails.setEmail("aNdReW@tImOtHy.com");
        proDetails = adminController.reactivateOrcidRecord(mockRequest, mockResponse, proDetails);
        assertEquals(0, proDetails.getErrors().size());

        deactivated = profileDao.find("4444-4444-4444-4445");
        assertNull(deactivated.getDeactivationDate());

        // Try to reactivate an already active account
        proDetails = adminController.reactivateOrcidRecord(mockRequest, mockResponse, proDetails);
        assertEquals(1, proDetails.getErrors().size());
        assertEquals(adminController.getMessage("admin.profile_reactivation.errors.already_active", new ArrayList<String>()), proDetails.getErrors().get(0));
        
        // Test deactivate
        result = adminController.deactivateOrcidRecords(mockRequest, mockResponse, "https://orcid.org/4444-4444-4444-4445");
        assertEquals(1, result.get("success").size());

        // Test deactivate
        proDetails.setOrcid("https://orcid.org/4444-4444-4444-4445");
        proDetails = adminController.reactivateOrcidRecord(mockRequest, mockResponse, proDetails);
        assertEquals(1, result.get("success").size());
        
    }
    
    @Test
    public void preventDisablingMembersTest() throws IllegalAccessException, UnsupportedEncodingException {
        ProfileHistoryEventManager profileHistoryEventManager = Mockito.mock(ProfileHistoryEventManagerImpl.class);
        ProfileEntityManager profileEntityManager = (ProfileEntityManager) ReflectionTestUtils.getField(adminController, "profileEntityManager");
        ReflectionTestUtils.setField(profileEntityManager, "profileHistoryEventManager", profileHistoryEventManager);
        Mockito.doNothing().when(profileHistoryEventManager).recordEvent(Mockito.any(ProfileHistoryEventType.class), Mockito.anyString(), Mockito.anyString());
        
        // Test deactivate
        Map<String, Set<String>> result = adminController.deactivateOrcidRecords(mockRequest, mockResponse, "5555-5555-5555-5558");
        assertEquals(0, result.get("notFoundList").size());
        assertEquals(0, result.get("alreadyDeactivated").size());
        assertEquals(0, result.get("success").size());
        assertEquals(1, result.get("members").size());
        assertTrue(result.get("members").contains("5555-5555-5555-5558"));
    }

    @Test
    public void findIdsTest() {
        Map<String, String> ids = adminController.findIdByEmailHelper("spike@milligan.com,michael@bentine.com,peter@sellers.com,mixed@case.com,invalid@email.com");
        assertNotNull(ids);
        assertEquals(4, ids.size());
        assertTrue(ids.containsKey("spike@milligan.com"));
        assertEquals("4444-4444-4444-4441", ids.get("spike@milligan.com"));
        assertTrue(ids.containsKey("michael@bentine.com"));
        assertEquals("4444-4444-4444-4442", ids.get("michael@bentine.com"));
        assertTrue(ids.containsKey("peter@sellers.com"));
        assertEquals("4444-4444-4444-4443", ids.get("peter@sellers.com"));
        assertTrue(ids.containsKey("mixed@case.com"));
        assertEquals("4444-4444-4444-4442", ids.get("mixed@case.com"));
        assertFalse(ids.containsKey("invalid@email.com"));
    }

    @Test
    public void resetPasswordTest() throws IllegalAccessException, UnsupportedEncodingException {
        ProfileEntity p = profileEntityManager.findByOrcid("4444-4444-4444-4441");
        assertEquals("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=", p.getPassword());
        AdminChangePassword form = new AdminChangePassword();
        form.setOrcidOrEmail("4444-4444-4444-4441");
        form.setPassword("password1");
        adminController.resetPassword(mockRequest, mockResponse, form);
        p = profileEntityManager.findByOrcid("4444-4444-4444-4441");
        assertFalse("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=".equals(p.getPassword()));
    }

    @Test
    public void resetPasswordUsingEmailTest() throws IllegalAccessException, UnsupportedEncodingException {
        ProfileEntity p = profileEntityManager.findByOrcid("4444-4444-4444-4442");
        assertEquals("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=", p.getPassword());
        AdminChangePassword form = new AdminChangePassword();
        form.setOrcidOrEmail("michael@bentine.com");
        form.setPassword("password1");
        adminController.resetPassword(mockRequest, mockResponse, form);
        p = profileEntityManager.findByOrcid("4444-4444-4444-4442");
        assertFalse("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=".equals(p.getPassword()));
    }
    
    @Test
    public void resetPasswordTestOrcidURL() throws IllegalAccessException, UnsupportedEncodingException {
        ProfileEntity p = profileEntityManager.findByOrcid("4444-4444-4444-4443");
        assertEquals("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=", p.getPassword());
        AdminChangePassword form = new AdminChangePassword();
        form.setOrcidOrEmail("https://orcid.org/4444-4444-4444-4443");
        form.setPassword("password1");
        adminController.resetPassword(mockRequest, mockResponse, form);
        p = profileEntityManager.findByOrcid("4444-4444-4444-4443");
        assertFalse("e9adO9I4UpBwqI5tGR+qDodvAZ7mlcISn+T+kyqXPf2Z6PPevg7JijqYr6KGO8VOskOYqVOEK2FEDwebxWKGDrV/TQ9gRfKWZlzxssxsOnA=".equals(p.getPassword()));
    }

    @Test
    public void verifyEmailTest() throws Exception {
        TargetProxyHelper.injectIntoProxy(emailManager, "notificationManager", mockNotificationManager);
        TargetProxyHelper.injectIntoProxy(adminController, "emailManagerReadOnly", mockEmailManagerReadOnly);
        when(mockEmailManagerReadOnly.findOrcidIdByEmail("not-verified@email.com")).thenReturn("4444-4444-4444-4499");
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        
        // Add not verified email
        Email email = new Email();
        email.setEmail("not-verified@email.com");
        email.setCurrent(false);
        email.setPrimary(false);
        email.setVerified(false);
        email.setVisibility(Visibility.PUBLIC);
        emailManager.addEmail(request, "4444-4444-4444-4499", email);

        // Verify the email
        adminController.adminVerifyEmail(request, response, "not-verified@email.com");
        EmailEntity emailEntity = emailManager.find("not-verified@email.com");
        assertNotNull(emailEntity);
        assertTrue(emailEntity.getVerified());
        TargetProxyHelper.injectIntoProxy(emailManager, "notificationManager", notificationManager);
    }

    @Test
    public void testLockAccounts() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        

        AdminController adminController = new AdminController();        
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);

        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-locked-email1@test.com,record-locked-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com,reviewed-email@test.com,0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";

        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);

        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-locked-email1@test.com", "record-locked-email1@test.com");
        map.put("record-locked-email2@test.com", "record-locked-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");
        map.put("reviewed-email@test.com", "reviewed-email@test.com");

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);

        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {
            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-locked-email1@test.com") || ar1.equals("record-locked-email2@test.com") 
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setRecordLocked(true);
                } else {
                    p.setRecordLocked(false);
                }

                if (ar1.contentEquals("reviewed-email@test.com")) {
                    p.setReviewed(true);
                } else {
                    p.setReviewed(false);
                }
                return p;
            }

        });

        LockAccounts lockAccounts = new LockAccounts();
        lockAccounts.setOrcidsToLock(commaSeparatedValues);
        lockAccounts.setLockReason(LockReason.SPAM.getLabel());

        Map<String, Set<String>> results = adminController.lockRecords(mockRequest, mockResponse, lockAccounts);
        assertEquals(3, results.get("notFound").size());
        assertTrue(results.get("notFound").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFound").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFound").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyLocked").size());
        assertTrue(results.get("alreadyLocked").contains("record-locked-email1@test.com"));
        assertTrue(results.get("alreadyLocked").contains("record-locked-email2@test.com"));
        assertTrue(results.get("alreadyLocked").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyLocked").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("successful").size());
        assertTrue(results.get("successful").contains("successful-email1@test.com"));
        assertTrue(results.get("successful").contains("successful-email2@test.com"));
        assertTrue(results.get("successful").contains("successful-email3@test.com"));
        assertTrue(results.get("successful").contains("successful-email4@test.com"));
        assertTrue(results.get("successful").contains("0000-0000-0000-0003"));
        assertTrue(results.get("successful").contains("https://orcid.org/0000-0000-0000-0004"));
        

        assertEquals(1, results.get("reviewed").size());
        assertTrue(results.get("reviewed").contains("reviewed-email@test.com"));

        Mockito.verify(emailManager, Mockito.times(9)).emailExists(Mockito.anyString());
        Mockito.verify(profileEntityManager, Mockito.times(6)).lockProfile(Mockito.anyString(), Mockito.anyString(), isNull());
    }

    @Test
    public void testUnlockAccounts() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        

        AdminController adminController = new AdminController();        
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);

        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-unlocked-email1@test.com,record-unlocked-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com, 0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";

        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-unlocked-email1@test.com", "record-unlocked-email1@test.com");
        map.put("record-unlocked-email2@test.com", "record-unlocked-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);

        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {
            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-unlocked-email1@test.com") || ar1.equals("record-unlocked-email2@test.com")
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setRecordLocked(false);
                } else {
                    p.setRecordLocked(true);
                }

                return p;
            }

        });

        Map<String, Set<String>> results = adminController.unlockRecords(mockRequest, mockResponse, commaSeparatedValues);
        assertEquals(3, results.get("notFound").size());
        assertTrue(results.get("notFound").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFound").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFound").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyUnlocked").size());
        assertTrue(results.get("alreadyUnlocked").contains("record-unlocked-email1@test.com"));
        assertTrue(results.get("alreadyUnlocked").contains("record-unlocked-email2@test.com"));
        assertTrue(results.get("alreadyUnlocked").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyUnlocked").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("successful").size());
        assertTrue(results.get("successful").contains("successful-email1@test.com"));
        assertTrue(results.get("successful").contains("successful-email2@test.com"));
        assertTrue(results.get("successful").contains("successful-email3@test.com"));
        assertTrue(results.get("successful").contains("successful-email4@test.com"));
        assertTrue(results.get("successful").contains("0000-0000-0000-0003"));
        assertTrue(results.get("successful").contains("https://orcid.org/0000-0000-0000-0004"));

        Mockito.verify(emailManager, Mockito.times(8)).emailExists(Mockito.anyString());
        Mockito.verify(profileEntityManager, Mockito.times(6)).unlockProfile(Mockito.anyString());
    }

    @Test
    public void testReviewAccounts() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        SpamManager spamManager = Mockito.mock(SpamManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        

        AdminController adminController = new AdminController();        
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "spamManager", spamManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);

        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-reviewed-email1@test.com,record-reviewed-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com,0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";

        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-reviewed-email1@test.com", "record-reviewed-email1@test.com");
        map.put("record-reviewed-email2@test.com", "record-reviewed-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");              

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {

            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-reviewed-email1@test.com") || ar1.equals("record-reviewed-email2@test.com")
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setReviewed(true);
                } else {
                    p.setReviewed(false);
                }
                return p;
            }

        });

        Mockito.when(profileEntityManager.reviewProfile("not-found-email1@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review null profile"));
        Mockito.when(profileEntityManager.reviewProfile("not-found-email2@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review null profile"));
        Mockito.when(profileEntityManager.reviewProfile("record-reviewed-email1@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("record-reviewed-email2@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));        

        Mockito.when(profileEntityManager.reviewProfile("successful-email1@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email2@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email3@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email4@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0001")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0002")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0003")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0004")).thenReturn(true);

        Map<String, Set<String>> results = adminController.reviewRecords(mockRequest, mockResponse, commaSeparatedValues);
        assertEquals(3, results.get("notFound").size());
        assertTrue(results.get("notFound").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFound").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFound").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyReviewed").size());
        assertTrue(results.get("alreadyReviewed").contains("record-reviewed-email1@test.com"));
        assertTrue(results.get("alreadyReviewed").contains("record-reviewed-email2@test.com"));
        assertTrue(results.get("alreadyReviewed").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyReviewed").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("successful").size());
        assertTrue(results.get("successful").contains("successful-email1@test.com"));
        assertTrue(results.get("successful").contains("successful-email2@test.com"));
        assertTrue(results.get("successful").contains("successful-email3@test.com"));
        assertTrue(results.get("successful").contains("successful-email4@test.com"));
        assertTrue(results.get("successful").contains("0000-0000-0000-0003"));
        assertTrue(results.get("successful").contains("https://orcid.org/0000-0000-0000-0004"));

        Mockito.verify(emailManager, Mockito.times(8)).emailExists(Mockito.anyString());
        Mockito.verify(profileEntityManager, Mockito.times(6)).reviewProfile(Mockito.anyString());
    }

    @Test
    public void testUnreviewAccounts() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        

        AdminController adminController = new AdminController();        
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);

        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-unreviewed-email1@test.com,record-unreviewed-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com,0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";

        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-unreviewed-email1@test.com", "record-unreviewed-email1@test.com");
        map.put("record-unreviewed-email2@test.com", "record-unreviewed-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {

            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-unreviewed-email1@test.com") || ar1.equals("record-unreviewed-email2@test.com")
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setReviewed(false);
                } else {
                    p.setReviewed(true);
                }
                return p;
            }

        });

        Mockito.when(profileEntityManager.reviewProfile("not-found-email1@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review null profile"));
        Mockito.when(profileEntityManager.reviewProfile("not-found-email2@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review null profile"));
        Mockito.when(profileEntityManager.reviewProfile("record-unreviewed-email1@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("record-unreviewed-email2@test.com")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0001")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0002")).thenThrow(new RuntimeException("Controller shouldn't try to review reviewed profile"));
        Mockito.when(profileEntityManager.reviewProfile("successful-email1@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email2@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email3@test.com")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("successful-email4@test.com")).thenReturn(true);       
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0003")).thenReturn(true);
        Mockito.when(profileEntityManager.reviewProfile("0000-0000-0000-0004")).thenReturn(true);

        Map<String, Set<String>> results = adminController.unreviewRecords(mockRequest, mockResponse, commaSeparatedValues);
        assertEquals(3, results.get("notFound").size());
        assertTrue(results.get("notFound").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFound").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFound").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyUnreviewed").size());
        assertTrue(results.get("alreadyUnreviewed").contains("record-unreviewed-email1@test.com"));
        assertTrue(results.get("alreadyUnreviewed").contains("record-unreviewed-email2@test.com"));
        assertTrue(results.get("alreadyUnreviewed").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyUnreviewed").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("successful").size());
        assertTrue(results.get("successful").contains("successful-email1@test.com"));
        assertTrue(results.get("successful").contains("successful-email2@test.com"));
        assertTrue(results.get("successful").contains("successful-email3@test.com"));
        assertTrue(results.get("successful").contains("successful-email4@test.com"));
        assertTrue(results.get("successful").contains("0000-0000-0000-0003"));
        assertTrue(results.get("successful").contains("https://orcid.org/0000-0000-0000-0004"));


        Mockito.verify(emailManager, Mockito.times(8)).emailExists(Mockito.anyString());
        Mockito.verify(profileEntityManager, Mockito.times(6)).unreviewProfile(Mockito.anyString());
    }

    @Test
    public void testGetLockReasons() {
        AdminManager adminManager = Mockito.mock(AdminManager.class);
        AdminController adminController = new AdminController();
        ReflectionTestUtils.setField(adminController, "adminManager", adminManager);
        TargetProxyHelper.injectIntoProxy(adminController, "orcidSecurityManager", mockOrcidSecurityManager);
        
        adminController.getLockReasons();

        Mockito.verify(adminManager, Mockito.times(1)).getLockReasons();
    }

    @Test
    public void resendClaimEmail() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);
        NotificationManager notificationManager = Mockito.mock(NotificationManager.class);
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        

        AdminController adminController = new AdminController();
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);                                                                    
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "notificationManager", notificationManager);

        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-claimed-email1@test.com,record-claimed-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com,0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";
        
        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-claimed-email1@test.com", "record-claimed-email1@test.com");
        map.put("record-claimed-email2@test.com", "record-claimed-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");              

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {

            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-claimed-email1@test.com") || ar1.equals("record-claimed-email2@test.com")
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setClaimed(true);
                } else {
                    p.setClaimed(false);
                }
                return p;
            }

        });              
      
        Map<String, List<String>> results = adminController.resendClaimEmail(mockRequest, mockResponse, commaSeparatedValues);
        assertEquals(3, results.get("notFound").size());
        assertTrue(results.get("notFound").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFound").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFound").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyClaimed").size());
        assertTrue(results.get("alreadyClaimed").contains("record-claimed-email1@test.com"));
        assertTrue(results.get("alreadyClaimed").contains("record-claimed-email2@test.com"));
        assertTrue(results.get("alreadyClaimed").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyClaimed").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("successful").size());
        assertTrue(results.get("successful").contains("successful-email1@test.com"));
        assertTrue(results.get("successful").contains("successful-email2@test.com"));
        assertTrue(results.get("successful").contains("successful-email3@test.com"));
        assertTrue(results.get("successful").contains("successful-email4@test.com"));
        assertTrue(results.get("successful").contains("0000-0000-0000-0003"));
        assertTrue(results.get("successful").contains("https://orcid.org/0000-0000-0000-0004"));

        Mockito.verify(emailManager, Mockito.times(8)).emailExists(Mockito.anyString());        
        Mockito.verify(notificationManager, Mockito.times(6)).sendApiRecordCreationEmail(Mockito.nullable(String.class), Mockito.anyString());
    }

    @Test
    public void deactivateOrcidRecords() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);        
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);

        AdminController adminController = new AdminController();
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);        

        String commaSeparatedValues = "not-found-email1@test.com,not-found-email2@test.com,record-deactivated-email1@test.com,record-deactivated-email2@test.com,successful-email1@test.com,successful-email2@test.com,successful-email3@test.com,successful-email4@test.com,0000-0000-0000-0001,https://orcid.org/0000-0000-0000-0002,0000-0000-0000-0003,https://orcid.org/0000-0000-0000-0004,notAnOrcidIdOrEmail";
        
        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);

        Map<String, String> map = new HashMap<String, String>();
        map.put("record-deactivated-email1@test.com", "record-deactivated-email1@test.com");
        map.put("record-deactivated-email2@test.com", "record-deactivated-email2@test.com");
        map.put("successful-email1@test.com", "successful-email1@test.com");
        map.put("successful-email2@test.com", "successful-email2@test.com");
        map.put("successful-email3@test.com", "successful-email3@test.com");
        map.put("successful-email4@test.com", "successful-email4@test.com");              

        Mockito.when(emailManager.findOricdIdsByCommaSeparatedEmails(Mockito.anyString())).thenReturn(map);
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {

            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("record-deactivated-email1@test.com") || ar1.equals("record-deactivated-email2@test.com")
                        || ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {
                    p.setDeactivationDate(new Date());
                } else {
                    p.setDeactivationDate(null);
                }
                return p;
            }

        });              
      
        Map<String, Set<String>> results = adminController.deactivateOrcidRecords(mockRequest, mockResponse, commaSeparatedValues);
        assertEquals(3, results.get("notFoundList").size());
        assertTrue(results.get("notFoundList").contains("not-found-email1@test.com"));
        assertTrue(results.get("notFoundList").contains("not-found-email2@test.com"));
        assertTrue(results.get("notFoundList").contains("notAnOrcidIdOrEmail"));

        assertEquals(4, results.get("alreadyDeactivated").size());
        assertTrue(results.get("alreadyDeactivated").contains("record-deactivated-email1@test.com"));
        assertTrue(results.get("alreadyDeactivated").contains("record-deactivated-email2@test.com"));
        assertTrue(results.get("alreadyDeactivated").contains("0000-0000-0000-0001"));
        assertTrue(results.get("alreadyDeactivated").contains("https://orcid.org/0000-0000-0000-0002"));

        assertEquals(6, results.get("success").size());
        assertTrue(results.get("success").contains("successful-email1@test.com"));
        assertTrue(results.get("success").contains("successful-email2@test.com"));
        assertTrue(results.get("success").contains("successful-email3@test.com"));
        assertTrue(results.get("success").contains("successful-email4@test.com"));
        assertTrue(results.get("success").contains("0000-0000-0000-0003"));
        assertTrue(results.get("success").contains("https://orcid.org/0000-0000-0000-0004"));

        Mockito.verify(emailManager, Mockito.times(8)).emailExists(Mockito.anyString());        
        Mockito.verify(profileEntityManager, Mockito.times(6)).deactivateRecord(Mockito.anyString());

    }

    @Test
    public void startDelegationProcess() throws Exception {
        
        AdminManager adminManager = Mockito.mock(AdminManager.class);                
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);        
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);        
        LocaleManager localeManager = Mockito.mock(LocaleManager.class);                                                
        
        AdminController adminController = new AdminController();
        ReflectionTestUtils.setField(adminController, "adminManager", adminManager);                                                                       
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);        
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);
        ReflectionTestUtils.setField(adminController, "localeManager", localeManager);
        
        AdminDelegatesRequest adminDelegatesRequest = new AdminDelegatesRequest(); 
        Text trusted = new Text();
        trusted.setValue("https://orcid.org/0000-0000-0000-00020000-0000-0000-0001");
        Text managed = new Text();
        managed.setValue("0000-0000-0000-0002");
        adminDelegatesRequest.setTrusted(trusted);       
        adminDelegatesRequest.setManaged(managed);                      
        
        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
                       
        Mockito.when(localeManager.resolveMessage(Mockito.anyString(), Mockito.any())).thenReturn("Email or ORCID iD is 0000-0000-0000-0001 invalid");                                     
        
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);
        Mockito.when(profileEntityCacheManager.retrieve(Mockito.anyString())).thenAnswer(new Answer<ProfileEntity>() {

            @Override
            public ProfileEntity answer(InvocationOnMock invocation) throws Throwable {
                String ar1 = invocation.getArgument(0);
                ProfileEntity p = new ProfileEntity();
                p.setId(ar1);
                if (ar1.equals("0000-0000-0000-0001") || ar1.equals("0000-0000-0000-0002")) {                    
                    p.setRecordLocked(false);                                   
                } else {
                    p.setRecordLocked(true);
                }
                return p;
            }

        });              
      
        adminController.startDelegationProcess(mockRequest, mockResponse, adminDelegatesRequest);
                
        Mockito.verify(adminManager, Mockito.times(1)).startDelegationProcess(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        
        adminDelegatesRequest = new AdminDelegatesRequest(); 
        trusted = new Text();
        trusted.setValue("not-found-email1@test.com");
        managed = new Text();
        managed.setValue("not-found-email2@test.com");
        adminDelegatesRequest.setTrusted(trusted);       
        adminDelegatesRequest.setManaged(managed);   
        
        AdminDelegatesRequest results = adminController.startDelegationProcess(mockRequest, mockResponse, adminDelegatesRequest);

        assertEquals(1, results.getManaged().getErrors().size());
        assertEquals(1, results.getTrusted().getErrors().size());
    }
    
    @Test
    public void adminSwitchUser() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);        
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);

        AdminController adminController = new AdminController();
               
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);                
        
        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);                            
        
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);                
      
        Map<String, String> results = adminController.adminSwitchUser(mockRequest, mockResponse, "not-found-email1@test.com");

        assertEquals("Invalid id not-found-email1@test.com", results.get("errorMessg"));        
   
        results = adminController.adminSwitchUser(mockRequest, mockResponse, "not-found-email2@test.com");
        
        assertEquals("Invalid id not-found-email2@test.com", results.get("errorMessg"));
        
        results = adminController.adminSwitchUser(mockRequest, mockResponse, "0000-0000-0000-0001");
        
        assertEquals("0000-0000-0000-0001", results.get("id"));
        
        results = adminController.adminSwitchUser(mockRequest, mockResponse, "https://orcid.org/0000-0000-0000-0002");
        
        assertEquals("0000-0000-0000-0002", results.get("id"));

    }     
 
    @Test
    public void resetPasswordValidateId() throws Exception {
        ProfileEntityCacheManager profileEntityCacheManager = Mockito.mock(ProfileEntityCacheManager.class);
        ProfileEntityManager profileEntityManager = Mockito.mock(ProfileEntityManager.class);
        EmailManager emailManager = Mockito.mock(EmailManager.class);        
        OrcidSecurityManager orcidSecurityManager = Mockito.mock(OrcidSecurityManager.class);
        LocaleManager localeManager = Mockito.mock(LocaleManager.class);                                                
        
        AdminController adminController = new AdminController();
               
        ReflectionTestUtils.setField(adminController, "orcidSecurityManager", orcidSecurityManager);
        ReflectionTestUtils.setField(adminController, "profileEntityManager", profileEntityManager);
        ReflectionTestUtils.setField(adminController, "emailManager", emailManager);
        ReflectionTestUtils.setField(adminController, "profileEntityCacheManager", profileEntityCacheManager);
        ReflectionTestUtils.setField(adminController, "localeManager", localeManager);
        
        Mockito.when(orcidSecurityManager.isAdmin()).thenReturn(true);
        
        Mockito.when(emailManager.emailExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(emailManager.emailExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);                            
        
        Mockito.when(localeManager.resolveMessage(Mockito.anyString(), Mockito.any())).thenReturn("That ORCID iD is not on our records");       
        
        Mockito.when(profileEntityManager.orcidExists(Mockito.anyString())).thenReturn(true);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email1@test.com"))).thenReturn(false);
        Mockito.when(profileEntityManager.orcidExists(Mockito.eq("not-found-email2@test.com"))).thenReturn(false);                
      
        AdminChangePassword adminChangePassword = new AdminChangePassword();
        adminChangePassword.setOrcidOrEmail("0000-0000-0000-0001");
        
        AdminChangePassword results = adminController.resetPasswordValidateId(mockRequest, mockResponse, adminChangePassword);        

        assertEquals(null, results.getError());        

        adminChangePassword = new AdminChangePassword();
        adminChangePassword.setOrcidOrEmail("https://orcid.org/0000-0000-0000-0002");
        
        results = adminController.resetPasswordValidateId(mockRequest, mockResponse, adminChangePassword);        

        assertEquals(null, results.getError());   
        
        adminChangePassword = new AdminChangePassword();
        adminChangePassword.setOrcidOrEmail("not-found-email1@test.com");
        
        results = adminController.resetPasswordValidateId(mockRequest, mockResponse, adminChangePassword);
        
        assertEquals("That ORCID iD is not on our records", results.getError());
        
        adminChangePassword = new AdminChangePassword();
        adminChangePassword.setOrcidOrEmail("not-found-email2@test.com");
        
        results = adminController.resetPasswordValidateId(mockRequest, mockResponse, adminChangePassword);
        
        assertEquals("That ORCID iD is not on our records", results.getError());        

    }
    
    @Test
    public void testDeactivateClient() throws ClientAlreadyDeactivatedException {
        SecurityContextHolder.getContext().setAuthentication(getAuthentication());
        Mockito.doNothing().when(clientDetailsManager).deactivateClientDetails(Mockito.eq("test"), Mockito.eq("4444-4444-4444-4440"));
        ClientActivationRequest clientDeactivation = new ClientActivationRequest();
        clientDeactivation.setClientId("test");
        clientDeactivation = adminController.deactivateClient(clientDeactivation);
        assertNull(clientDeactivation.getError());
    }
    
    @Test
    public void testActivateClient() throws ClientAlreadyActiveException {
        Mockito.doNothing().when(clientDetailsManager).activateClientDetails(Mockito.eq("test"));
        ClientActivationRequest clientActivation = new ClientActivationRequest();
        clientActivation.setClientId("test");
        clientActivation = adminController.activateClient(clientActivation);
        assertNull(clientActivation.getError());
    }
    
    @Test
    public void testDeactivateClientAlreadyDeactivated() throws ClientAlreadyDeactivatedException {
        SecurityContextHolder.getContext().setAuthentication(getAuthentication());
        Mockito.doThrow(new ClientAlreadyDeactivatedException("already-deactivated")).when(clientDetailsManager).deactivateClientDetails(Mockito.eq("test"), Mockito.eq("4444-4444-4444-4440"));
        ClientActivationRequest clientDeactivation = new ClientActivationRequest();
        clientDeactivation.setClientId("test");
        clientDeactivation = adminController.deactivateClient(clientDeactivation);
        assertNotNull(clientDeactivation.getError());
        assertEquals("already-deactivated", clientDeactivation.getError());
    }
    
    @Test
    public void testActivateClientAlreadyActive() throws ClientAlreadyActiveException {
        Mockito.doThrow(new ClientAlreadyActiveException("already-active")).when(clientDetailsManager).activateClientDetails(Mockito.eq("test"));
        ClientActivationRequest clientActivation = new ClientActivationRequest();
        clientActivation.setClientId("test");
        clientActivation = adminController.activateClient(clientActivation);
        assertNotNull(clientActivation.getError());
        assertEquals("already-active", clientActivation.getError());
    }

}