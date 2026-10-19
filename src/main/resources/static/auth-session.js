(function () {
    "use strict";

    const TOKEN_KEY = "spendwiseJwt";
    const USER_KEY = "spendwiseUser";
    const LOGIN_PAGE = "/login.html";

    const currentPage = window.location.pathname.toLowerCase();

    const authenticationPages = [
        "/login.html",
        "/register.html",
        "/forgot-password.html",
        "/reset-password.html"
    ];

    const isAuthenticationPage =
        authenticationPages.includes(currentPage);

    const originalFetch = window.fetch.bind(window);

    let authenticationVerified = false;
    let redirectingToLogin = false;

    /*
     * Retrieve the JWT.
     */
    function getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    }

    /*
     * Clear locally stored authentication information.
     */
    function clearAuthentication() {
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);
    }

    /*
     * Redirect to the login page.
     */
    function redirectToLogin() {
        if (redirectingToLogin) {
            return;
        }

        redirectingToLogin = true;

        clearAuthentication();

        if (!isAuthenticationPage) {
            window.location.replace(LOGIN_PAGE);
        }
    }

    /*
     * Identify public authentication endpoints.
     */
    function isPublicAuthenticationEndpoint(pathname) {
        return [
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/guest",
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
        ].includes(pathname);
    }

    /*
     * Identify API requests.
     */
    function isApiRequest(pathname) {
        return pathname.startsWith("/api/") ||
            [
                "/expenses",
                "/income",
                "/budgets",
                "/savings",
                "/categories",
                "/ai"
            ].some(function (path) {
                return pathname === path ||
                    pathname.startsWith(path + "/");
            });
    }

    /*
     * Attach the JWT to protected API requests.
     *
     * Existing fetch calls in the application can continue
     * using their original syntax.
     */
    window.fetch = async function (input, init) {
        const requestUrl =
            input instanceof Request ? input.url : input;

        const url = new URL(
            requestUrl,
            window.location.origin
        );

        const requestInit = {
            ...(input instanceof Request ? {
                method: input.method,
                headers: input.headers,
                body: input.body,
                credentials: input.credentials,
                cache: input.cache,
                redirect: input.redirect,
                referrer: input.referrer,
                referrerPolicy: input.referrerPolicy,
                integrity: input.integrity,
                keepalive: input.keepalive,
                mode: input.mode,
                signal: input.signal
            } : {}),
            ...(init || {})
        };

        const headers = new Headers(requestInit.headers);

        const sameOrigin =
            url.origin === window.location.origin;

        const apiRequest = isApiRequest(url.pathname);

        const publicAuthRequest =
            isPublicAuthenticationEndpoint(url.pathname);

        const token = getToken();

        if (
            sameOrigin &&
            apiRequest &&
            token &&
            !publicAuthRequest
        ) {
            headers.set(
                "Authorization",
                "Bearer " + token
            );
        }

        requestInit.headers = headers;

        /*
         * A Request body may already be consumed or locked.
         * Avoid reconstructing it unnecessarily.
         */
        if (
            input instanceof Request &&
            input.body &&
            requestInit.body === input.body
        ) {
            delete requestInit.body;
        }

        const response = await originalFetch(
            input,
            requestInit
        );

        /*
         * Redirect on 401 responses from protected APIs.
         */
        if (
            response.status === 401 &&
            sameOrigin &&
            apiRequest &&
            !publicAuthRequest &&
            !isAuthenticationPage
        ) {
            redirectToLogin();
        }

        return response;
    };

    /*
     * Add a logout button to authenticated pages.
     */
    function addLogoutButton() {
        if (isAuthenticationPage) {
            return;
        }

        if (
            document.getElementById(
                "spendwiseLogoutButton"
            )
        ) {
            return;
        }

        const style = document.createElement("style");

        style.textContent = `
            #spendwiseLogoutContainer {
                display: flex;
                justify-content: flex-end;
                align-items: center;
                gap: 12px;
                padding: 12px 20px;
                background: #ffffff;
                border-bottom: 1px solid #e2e8f0;
                font-family: Arial, sans-serif;
            }

            #spendwiseLogoutLabel {
                color: #475569;
                font-size: 14px;
                font-weight: 600;
                margin-right: auto;
            }

            #spendwiseLogoutError {
                color: #b91c1c;
                font-size: 13px;
            }

            #spendwiseLogoutButton {
                background: #dc2626;
                color: white;
                border: none;
                border-radius: 8px;
                padding: 10px 18px;
                font-size: 14px;
                font-weight: 600;
                cursor: pointer;
            }

            #spendwiseLogoutButton:hover {
                background: #b91c1c;
            }

            #spendwiseLogoutButton:disabled {
                opacity: 0.6;
                cursor: wait;
            }
        `;

        document.head.appendChild(style);

        const container = document.createElement("div");

        container.id = "spendwiseLogoutContainer";

        const label = document.createElement("span");
        label.id = "spendwiseLogoutLabel";
        label.textContent = "SpendWise AI";

        const error = document.createElement("span");
        error.id = "spendwiseLogoutError";
        error.setAttribute("role", "status");
        error.setAttribute("aria-live", "polite");

        const button = document.createElement("button");
        button.type = "button";
        button.id = "spendwiseLogoutButton";
        button.textContent = "Logout";

        container.append(label, error, button);

        document.body.insertBefore(
            container,
            document.body.firstChild
        );

        button.addEventListener("click", async function () {
            if (
                !window.confirm(
                    "Are you sure you want to log out?"
                )
            ) {
                return;
            }

            button.disabled = true;
            button.textContent = "Logging out...";
            error.textContent = "";

            try {
                const token = getToken();

                const headers = new Headers({
                    "Accept": "application/json"
                });

                if (token) {
                    headers.set(
                        "Authorization",
                        "Bearer " + token
                    );
                }

                /*
                 * Ask the backend to process logout.
                 * The frontend still clears its local token
                 * even if the endpoint fails.
                 */
                await originalFetch("/api/auth/logout", {
                    method: "POST",
                    headers: headers
                });

            } catch (exception) {
                console.error(
                    "Logout request failed:",
                    exception
                );

            } finally {
                clearAuthentication();

                window.location.replace(LOGIN_PAGE);
            }
        });
    }

    /*
     * Verify the token against the backend.
     */
    async function verifyAuthentication() {
        const token = getToken();

        if (!token) {
            redirectToLogin();
            return false;
        }

        try {
            const response = await originalFetch(
                "/api/auth/me",
                {
                    method: "GET",
                    headers: {
                        "Accept": "application/json",
                        "Authorization": "Bearer " + token
                    }
                }
            );

            if (!response.ok) {
                redirectToLogin();
                return false;
            }

            const user = await response.json();

            sessionStorage.setItem(
                USER_KEY,
                JSON.stringify(user)
            );

            authenticationVerified = true;

            return true;

        } catch (exception) {
            console.error(
                "Authentication verification failed:",
                exception
            );

            redirectToLogin();
            return false;
        }
    }

    /*
     * Initialize authentication checks.
     */
    async function initialize() {
        if (isAuthenticationPage) {
            return;
        }

        /*
         * Hide the page until authentication is verified.
         * This prevents protected content from briefly
         * appearing while verification is in progress.
         */
        document.documentElement.style.visibility = "hidden";

        const valid = await verifyAuthentication();

        if (!valid) {
            return;
        }

        addLogoutButton();

        document.documentElement.style.visibility = "visible";
    }

    /*
     * Start initialization.
     */
    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            initialize
        );
    } else {
        initialize();
    }

})();