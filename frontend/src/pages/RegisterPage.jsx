import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { UserPlus, Mail, Lock, User, ArrowRight, Eye, EyeOff } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { useAuth } from '../context/AuthContext';
import { GoogleAuthButton } from '../components/auth/GoogleAuthButton';

export function RegisterPage() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTarget = searchParams.get('redirect') || '/';
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!formData.email || !formData.password || !formData.firstName || !formData.lastName) {
      setErrorMessage('Please fill in all required fields.');
      return;
    }

    if (formData.password.length < 8) {
      setErrorMessage('Password must be at least 8 characters long.');
      return;
    }

    setIsSubmitting(true);
    const result = await register({
      email: formData.email,
      password: formData.password,
      firstName: formData.firstName,
      lastName: formData.lastName,
    });

    if (result.success) {
      // Auto login after successful registration
      const loginResult = await login(formData.email, formData.password);
      setIsSubmitting(false);
      if (loginResult.success) {
        navigate(redirectTarget, { replace: true });
      } else {
        navigate(`/login?registered=true${redirectTarget !== '/' ? `&redirect=${encodeURIComponent(redirectTarget)}` : ''}`);
      }
    } else {
      setIsSubmitting(false);
      setErrorMessage(result.error || 'Registration failed. Please try again.');
    }
  };

  return (
    <div style={{ padding: 'var(--space-12) 0 var(--space-16)' }}>
      <Container>
        <div style={{ maxWidth: '460px', margin: '0 auto' }}>
          <div className="card" style={{ padding: 'var(--space-8)' }}>
            <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 'var(--space-3)',
                }}
              >
                <UserPlus size={22} />
              </div>
              <h1 style={{ fontSize: 'var(--font-size-2xl)' }}>Create an Account</h1>
              <p style={{ fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
                Join ShopSphere to enjoy real-time shopping cart synchronization.
              </p>
            </div>

            {errorMessage && (
              <div
                style={{
                  padding: 'var(--space-3)',
                  backgroundColor: 'var(--color-error-bg)',
                  border: '1px solid var(--color-error-border)',
                  color: 'var(--color-error)',
                  fontSize: 'var(--font-size-sm)',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: 'var(--space-4)',
                }}
                role="alert"
              >
                {errorMessage}
              </div>
            )}

            {/* Google OAuth Sign Up */}
            <div style={{ marginBottom: 'var(--space-4)' }}>
              <GoogleAuthButton
                text="signup_with"
                redirectTarget={redirectTarget}
                onError={(err) => setErrorMessage(err)}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', margin: 'var(--space-4) 0', gap: 'var(--space-3)' }}>
              <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }} />
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                or register with email
              </span>
              <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }} />
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)' }}>
                <div>
                  <label
                    htmlFor="firstName"
                    style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', marginBottom: '6px', color: 'var(--color-text-secondary)' }}
                  >
                    First Name
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    required
                    value={formData.firstName}
                    onChange={handleChange}
                    placeholder="Alex"
                    style={{
                      width: '100%',
                      height: '40px',
                      padding: '0 12px',
                      fontSize: 'var(--font-size-sm)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)',
                      outline: 'none',
                    }}
                  />
                </div>

                <div>
                  <label
                    htmlFor="lastName"
                    style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', marginBottom: '6px', color: 'var(--color-text-secondary)' }}
                  >
                    Last Name
                  </label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    required
                    value={formData.lastName}
                    onChange={handleChange}
                    placeholder="Taylor"
                    style={{
                      width: '100%',
                      height: '40px',
                      padding: '0 12px',
                      fontSize: 'var(--font-size-sm)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>



              <div>
                <label
                  htmlFor="email"
                  style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', marginBottom: '6px', color: 'var(--color-text-secondary)' }}
                >
                  Email Address
                </label>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <Mail size={16} color="var(--color-text-muted)" style={{ position: 'absolute', left: '12px' }} />
                  <input
                    id="email"
                    name="email"
                    type="email"
                    required
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="alex@example.com"
                    style={{
                      width: '100%',
                      height: '40px',
                      paddingLeft: '38px',
                      paddingRight: '12px',
                      fontSize: 'var(--font-size-sm)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="password"
                  style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', marginBottom: '6px', color: 'var(--color-text-secondary)' }}
                >
                  Password
                </label>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <Lock size={16} color="var(--color-text-muted)" style={{ position: 'absolute', left: '12px' }} />
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="Create a strong password (min 8 chars)"
                    style={{
                      width: '100%',
                      height: '40px',
                      paddingLeft: '38px',
                      paddingRight: '40px',
                      fontSize: 'var(--font-size-sm)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)',
                      outline: 'none',
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute',
                      right: '10px',
                      background: 'none',
                      border: 'none',
                      color: 'var(--color-text-muted)',
                      cursor: 'pointer',
                      padding: '4px',
                      display: 'flex',
                      alignItems: 'center',
                    }}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isSubmitting}
                disabled={isSubmitting}
                icon={ArrowRight}
                iconPosition="right"
                style={{ width: '100%', marginTop: 'var(--space-2)' }}
              >
                {isSubmitting ? 'Registering...' : 'Complete Registration'}
              </Button>
            </form>

            <div style={{ marginTop: 'var(--space-6)', textAlign: 'center', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
              Already registered?{' '}
              <Link
                to={`/login${redirectTarget !== '/' ? `?redirect=${encodeURIComponent(redirectTarget)}` : ''}`}
                style={{ color: 'var(--color-secondary)', fontWeight: 'var(--font-weight-semibold)' }}
              >
                Sign In
              </Link>
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
