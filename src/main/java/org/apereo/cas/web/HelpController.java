package org.apereo.cas.web;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.support.oauth.web.endpoints.OAuth20ConfigurationContext;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.ticket.TicketState;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.JEEContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class HelpController {
    @Value("${res.server.url}")
    private String resServerUrl;//资源服务器的url

    @RequestMapping("/test")
    public String test() {
        return "ok";
    }
    //将资源服务器暴露的api通过cas返回给第三方
    @RequestMapping("/api/**")
    public ResponseEntity<String> resApi(HttpServletRequest request,HttpServletResponse response){
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        JEEContext context = new JEEContext(request, response, getOAuthConfigurationContext().getSessionStore());
        String accessToken = getAccessTokenFromRequest(request);
        if (StringUtils.isBlank(accessToken)) {
            //LOGGER.error("Missing [{}] from the request", OAuth20Constants.ACCESS_TOKEN);
            return buildUnauthorizedResponseEntity(OAuth20Constants.MISSING_ACCESS_TOKEN);
        }
        OAuth20AccessToken accessTokenTicket = getOAuthConfigurationContext().getTicketRegistry()
                .getTicket(accessToken, OAuth20AccessToken.class);

        if (accessTokenTicket == null || accessTokenTicket.isExpired()) {
            //LOGGER.error("Access token [{}] cannot be found in the ticket registry or has expired.", accessToken);
            if (accessTokenTicket != null) {
                getOAuthConfigurationContext().getTicketRegistry().deleteTicket(accessTokenTicket);
            }
            return expiredAccessTokenResponseEntity;
        }
        updateAccessTokenUsage(accessTokenTicket);
        //
        RestTemplate rt = new RestTemplate(new SSL());//new RestTemplate();
        String url = resServerUrl+request.getServletPath();
        Principal currentPrincipal = accessTokenTicket.getAuthentication().getPrincipal();
        LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>(1);
        params.add(OAuth20UserProfileViewRenderer.MODEL_ATTRIBUTE_ID,currentPrincipal.getId());
        System.out.println(currentPrincipal.getId());
        return  rt.postForEntity(url,null,String.class);
    }

    private final ResponseEntity<String> expiredAccessTokenResponseEntity;
    @Autowired
    private OAuth20ConfigurationContext oAuthConfigurationContext;

    public HelpController() {
        this.expiredAccessTokenResponseEntity = buildUnauthorizedResponseEntity(OAuth20Constants.EXPIRED_ACCESS_TOKEN);
    }

    /**
     * Build unauthorized response entity.
     *
     * @param code the code
     * @return the response entity
     */
    protected static ResponseEntity<String> buildUnauthorizedResponseEntity(final String code) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>(1);
        map.add(OAuth20Constants.ERROR, code);
        String value = OAuth20Utils.toJson(map);
        return new ResponseEntity<>(value, HttpStatus.UNAUTHORIZED);
    }
    public OAuth20ConfigurationContext getOAuthConfigurationContext(){
        return  oAuthConfigurationContext;
    }

    /**
     * Handle request internal response entity.
     *
     * @param request  the request
     * @param response the response
     * @return the response entity
     * @throws Exception the exception
     */
    @RequestMapping(path = OAuth20Constants.BASE_OAUTH20_URL + '/' + "b",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleGetRequest(final HttpServletRequest request,
                                                   final HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        JEEContext context = new JEEContext(request, response, getOAuthConfigurationContext().getSessionStore());
        String accessToken = getAccessTokenFromRequest(request);
        if (StringUtils.isBlank(accessToken)) {
            //LOGGER.error("Missing [{}] from the request", OAuth20Constants.ACCESS_TOKEN);
            return buildUnauthorizedResponseEntity(OAuth20Constants.MISSING_ACCESS_TOKEN);
        }
        OAuth20AccessToken accessTokenTicket = getOAuthConfigurationContext().getTicketRegistry()
                .getTicket(accessToken, OAuth20AccessToken.class);

        if (accessTokenTicket == null || accessTokenTicket.isExpired()) {
            //LOGGER.error("Access token [{}] cannot be found in the ticket registry or has expired.", accessToken);
            if (accessTokenTicket != null) {
                getOAuthConfigurationContext().getTicketRegistry().deleteTicket(accessTokenTicket);
            }
            return expiredAccessTokenResponseEntity;
        }
        updateAccessTokenUsage(accessTokenTicket);

        Map<String,Object> map = getOAuthConfigurationContext().getUserProfileDataCreator().createFrom(accessTokenTicket, context);
        return getOAuthConfigurationContext().getUserProfileViewRenderer().render(map, accessTokenTicket, response);
    }

    /**
     * Update the access token in the registry.
     *
     * @param accessTokenTicket the access token
     */
    protected void updateAccessTokenUsage(final OAuth20AccessToken accessTokenTicket) {
        TicketState accessTokenState = TicketState.class.cast(accessTokenTicket);
        accessTokenState.update();
        if (accessTokenTicket.isExpired()) {
            getOAuthConfigurationContext().getTicketRegistry().deleteTicket(accessTokenTicket.getId());
        } else {
            getOAuthConfigurationContext().getTicketRegistry().updateTicket(accessTokenTicket);
        }
    }

    /**
     * Gets access token from request.
     *
     * @param request the request
     * @return the access token from request
     */
    protected String getAccessTokenFromRequest(final HttpServletRequest request) {
        String accessToken = request.getParameter(OAuth20Constants.ACCESS_TOKEN);
        if (StringUtils.isBlank(accessToken)) {
            String authHeader = request.getHeader(HttpConstants.AUTHORIZATION_HEADER);
            if (StringUtils.isNotBlank(authHeader) && authHeader.toLowerCase()
                    .startsWith(OAuth20Constants.TOKEN_TYPE_BEARER.toLowerCase() + ' ')) {
                accessToken = authHeader.substring(OAuth20Constants.TOKEN_TYPE_BEARER.length() + 1);
            }
        }
        return accessToken;
    }
}

