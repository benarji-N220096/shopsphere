import React, { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const GOOGLE_CLIENT_ID = '665886355316-jf4jdkcjlcaiemfabqdqulhve2lj3f5s.apps.googleusercontent.com';

export function GoogleAuthButton({
  text = 'continue_with', // 'signin_with', 'signup_with', 'continue_with'
  redirectTarget = '/',
  onError
}) {
  const { loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const buttonRef = useRef(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [scriptLoaded, setScriptLoaded] = useState(false);

  const handleCredentialResponse = async (response) => {
    if (!response || !response.credential) {
      if (onError) onError('No credential received from Google.');
      return;
    }

    setIsProcessing(true);
    try {
      const result = await loginWithGoogle(response.credential);
      if (result.success) {
        navigate(redirectTarget, { replace: true });
      } else {
        if (onError) onError(result.error || 'Google authentication failed.');
      }
    } catch (err) {
      if (onError) onError(err.message || 'Google sign-in error.');
    } finally {
      setIsProcessing(false);
    }
  };

  useEffect(() => {
    let intervalId;

    const initGoogle = () => {
      if (window.google && window.google.accounts && window.google.accounts.id) {
        setScriptLoaded(true);
        try {
          window.google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleCredentialResponse,
            auto_select: false,
            cancel_on_tap_outside: true,
          });

          if (buttonRef.current) {
            buttonRef.current.innerHTML = ''; // clear before render
            window.google.accounts.id.renderButton(buttonRef.current, {
              theme: 'outline',
              size: 'large',
              width: 350,
              text: text === 'signup_with' ? 'signup_with' : (text === 'signin_with' ? 'signin_with' : 'continue_with'),
              shape: 'rectangular',
              logo_alignment: 'left',
            });
          }
        } catch (e) {
          console.warn('Google Identity initialization error:', e);
        }
        return true;
      }
      return false;
    };

    if (!initGoogle()) {
      intervalId = setInterval(() => {
        if (initGoogle()) {
          clearInterval(intervalId);
        }
      }, 300);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [text, redirectTarget]);

  const handleCustomClick = () => {
    if (window.google && window.google.accounts && window.google.accounts.id) {
      window.google.accounts.id.prompt();
    } else {
      if (onError) onError('Google Sign-In is initializing. Please try again in a few moments.');
    }
  };

  return (
    <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      {/* Official Google Rendered Button Container */}
      <div
        ref={buttonRef}
        style={{
          width: '100%',
          display: scriptLoaded ? 'flex' : 'none',
          justifyContent: 'center',
          minHeight: '44px'
        }}
      />

      {/* Fallback button if script is loading or rendering */}
      {(!scriptLoaded || isProcessing) && (
        <button
          type="button"
          onClick={handleCustomClick}
          disabled={isProcessing}
          style={{
            width: '100%',
            height: '44px',
            backgroundColor: '#ffffff',
            color: '#374151',
            border: '1px solid #d1d5db',
            borderRadius: 'var(--radius-sm, 6px)',
            fontSize: 'var(--font-size-sm, 14px)',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px',
            cursor: isProcessing ? 'not-allowed' : 'pointer',
            transition: 'background-color 0.15s ease, border-color 0.15s ease',
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
          }}
        >
          <svg width="18" height="18" viewBox="0 0 18 18">
            <path
              fill="#4285F4"
              d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.616z"
            />
            <path
              fill="#34A853"
              d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332C2.438 15.983 5.482 18 9 18z"
            />
            <path
              fill="#FBBC05"
              d="M3.964 10.707c-.18-.54-.282-1.117-.282-1.707s.102-1.167.282-1.707V4.961H.957C.347 6.175 0 7.55 0 9s.347 2.825.957 4.039l3.007-2.332z"
            />
            <path
              fill="#EA4335"
              d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0 5.482 0 2.438 2.017.957 4.961L3.964 7.293C4.672 5.166 6.656 3.58 9 3.58z"
            />
          </svg>
          <span>
            {isProcessing
              ? 'Connecting with Google...'
              : (text === 'signup_with' ? 'Sign up with Google' : 'Sign in with Google')}
          </span>
        </button>
      )}
    </div>
  );
}
