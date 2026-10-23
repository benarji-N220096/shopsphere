import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  CreditCard, 
  ShieldCheck, 
  Truck, 
  Lock, 
  AlertCircle, 
  ArrowLeft, 
  Check, 
  ShoppingBag,
  HelpCircle
} from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { orderApi } from '../services/api/orderApi';
import { formatCurrency } from '../utils/formatters';

export function CheckoutPage() {
  const { items, subtotal, clearLocalCart } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();

  // Form states
  const [deliveryInfo, setDeliveryInfo] = useState({
    fullName: user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : '',
    email: user?.email || '',
    address: 'Plot 42, Hitech City Main Road, Madhapur',
    city: 'Hyderabad',
    state: 'Telangana',
    postalCode: '500081',
    country: 'India',
  });

  const [paymentMethod, setPaymentMethod] = useState('CREDIT_CARD');
  const [mockPaymentAction, setMockPaymentAction] = useState('SUCCESS');
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const shippingCost = 0; // Free shipping
  const finalTotal = subtotal + shippingCost;

  const handleDeliveryChange = (e) => {
    setDeliveryInfo((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    if (items.length === 0) {
      setErrorMessage('Your cart is empty. Please add items before checking out.');
      return;
    }

    setErrorMessage('');
    setIsProcessing(true);

    try {
      const orderPayload = {
        paymentMethod: paymentMethod,
        mockPaymentAction: mockPaymentAction,
      };

      const response = await orderApi.createOrder(orderPayload);

      // Order created successfully
      clearLocalCart();
      setIsProcessing(false);

      if (response && response.id) {
        navigate(`/orders/success/${response.id}`, { state: { order: response } });
      } else {
        navigate('/orders');
      }
    } catch (err) {
      setIsProcessing(false);
      let userFriendlyMessage = err.message || 'We could not complete your order. Please try again.';

      if (err.status === 409 || err.message?.toLowerCase().includes('stock')) {
        userFriendlyMessage = 'Insufficient inventory for one or more items in your cart. Please review item quantities.';
      } else if (err.status === 402 || err.message?.toLowerCase().includes('payment')) {
        userFriendlyMessage = 'Payment processing failed. Please verify your payment details or choose another method.';
      } else if (err.status === 503) {
        userFriendlyMessage = 'The payment or order service is temporarily unavailable. Please try again in a few moments.';
      }

      setErrorMessage(userFriendlyMessage);
    }
  };

  if (items.length === 0) {
    return (
      <div style={{ padding: 'var(--space-16) 0' }}>
        <Container>
          <div className="card" style={{ maxWidth: '540px', margin: '0 auto', textAlign: 'center', padding: 'var(--space-10)' }}>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-primary-light)',
                color: 'var(--color-primary)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 'var(--space-4)',
              }}
            >
              <ShoppingBag size={32} />
            </div>
            <h1 style={{ fontSize: 'var(--font-size-2xl)', marginBottom: 'var(--space-2)' }}>Your Cart is Empty</h1>
            <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--space-6)' }}>
              You don't have any items to check out. Explore our product catalog to discover premium items.
            </p>
            <Link to="/products">
              <Button variant="primary" size="lg">
                Explore Products
              </Button>
            </Link>
          </div>
        </Container>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-8) 0 var(--space-16)' }}>
      <Container>
        <div style={{ marginBottom: 'var(--space-6)' }}>
          <Link
            to="/cart"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-secondary)',
              marginBottom: 'var(--space-3)',
            }}
          >
            <ArrowLeft size={16} /> Return to Shopping Cart
          </Link>
          <h1 style={{ fontSize: 'var(--font-size-3xl)' }}>Secure Checkout</h1>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
            Review your order items, delivery information, and complete your purchase.
          </p>
        </div>

        {errorMessage && (
          <div
            style={{
              padding: 'var(--space-4)',
              backgroundColor: 'var(--color-error-bg)',
              border: '1px solid var(--color-error-border)',
              color: 'var(--color-error)',
              borderRadius: 'var(--radius-md)',
              marginBottom: 'var(--space-6)',
              display: 'flex',
              alignItems: 'flex-start',
              gap: 'var(--space-3)',
            }}
            role="alert"
          >
            <AlertCircle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong style={{ display: 'block', marginBottom: '2px' }}>Order Submission Notice</strong>
              <span style={{ fontSize: 'var(--font-size-sm)' }}>{errorMessage}</span>
            </div>
          </div>
        )}

        <form onSubmit={handlePlaceOrder}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'minmax(0, 1.8fr) minmax(320px, 1.2fr)',
              gap: 'var(--space-8)',
              alignItems: 'start',
            }}
          >
            {/* Left Column: Delivery & Payment Details */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
              {/* Step 1: Customer / Delivery Info */}
              <div className="card" style={{ padding: 'var(--space-6)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-5)', borderBottom: '1px solid var(--color-border)', paddingBottom: 'var(--space-4)' }}>
                  <div
                    style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: 'var(--color-primary)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 'var(--font-size-sm)',
                      fontWeight: 'var(--font-weight-bold)',
                    }}
                  >
                    1
                  </div>
                  <div>
                    <h2 style={{ fontSize: 'var(--font-size-lg)', margin: 0 }}>Shipping & Delivery Information</h2>
                    <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>Verified customer address for delivery</span>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                      Recipient Full Name
                    </label>
                    <input
                      name="fullName"
                      type="text"
                      required
                      value={deliveryInfo.fullName}
                      onChange={handleDeliveryChange}
                      placeholder="Jane Doe"
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
                    <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                      Notification Email
                    </label>
                    <input
                      name="email"
                      type="email"
                      required
                      value={deliveryInfo.email}
                      onChange={handleDeliveryChange}
                      placeholder="jane@example.com"
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

                  <div style={{ gridColumn: 'span 2' }}>
                    <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                      Street Address
                    </label>
                    <input
                      name="address"
                      type="text"
                      required
                      value={deliveryInfo.address}
                      onChange={handleDeliveryChange}
                      placeholder="123 Market Street, Suite 400"
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
                    <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                      City
                    </label>
                    <input
                      name="city"
                      type="text"
                      required
                      value={deliveryInfo.city}
                      onChange={handleDeliveryChange}
                      placeholder="San Francisco"
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

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                        State / Province
                      </label>
                      <input
                        name="state"
                        type="text"
                        required
                        value={deliveryInfo.state}
                        onChange={handleDeliveryChange}
                        placeholder="CA"
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
                      <label style={{ display: 'block', fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)', marginBottom: '6px' }}>
                        Postal Code
                      </label>
                      <input
                        name="postalCode"
                        type="text"
                        required
                        value={deliveryInfo.postalCode}
                        onChange={handleDeliveryChange}
                        placeholder="94105"
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
                </div>
              </div>

              {/* Step 2: Payment Method */}
              <div className="card" style={{ padding: 'var(--space-6)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', marginBottom: 'var(--space-5)', borderBottom: '1px solid var(--color-border)', paddingBottom: 'var(--space-4)' }}>
                  <div
                    style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: 'var(--color-primary)',
                      color: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 'var(--font-size-sm)',
                      fontWeight: 'var(--font-weight-bold)',
                    }}
                  >
                    2
                  </div>
                  <div>
                    <h2 style={{ fontSize: 'var(--font-size-lg)', margin: 0 }}>Payment Method</h2>
                    <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>Select your preferred payment channel</span>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)', marginBottom: 'var(--space-4)' }}>
                  {[
                    { id: 'CREDIT_CARD', label: 'Credit Card', desc: 'Visa, MasterCard, Amex' },
                    { id: 'DEBIT_CARD', label: 'Debit Card', desc: 'Direct bank debit' },
                    { id: 'UPI', label: 'UPI / Instant Pay', desc: 'Instant mobile transfer' },
                    { id: 'CASH_ON_DELIVERY', label: 'Cash on Delivery', desc: 'Pay when delivered' },
                  ].map((method) => {
                    const isSelected = paymentMethod === method.id;
                    return (
                      <div
                        key={method.id}
                        onClick={() => setPaymentMethod(method.id)}
                        style={{
                          padding: 'var(--space-4)',
                          borderRadius: 'var(--radius-md)',
                          border: `2px solid ${isSelected ? 'var(--color-primary)' : 'var(--color-border)'}`,
                          backgroundColor: isSelected ? 'var(--color-primary-light)' : 'var(--color-surface)',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'flex-start',
                          justifyContent: 'space-between',
                          transition: 'all var(--transition-fast)',
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 'var(--font-weight-semibold)', fontSize: 'var(--font-size-sm)', color: isSelected ? 'var(--color-primary)' : 'var(--color-text)' }}>
                            {method.label}
                          </div>
                          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                            {method.desc}
                          </div>
                        </div>
                        {isSelected && (
                          <div
                            style={{
                              width: '18px',
                              height: '18px',
                              borderRadius: 'var(--radius-full)',
                              backgroundColor: 'var(--color-primary)',
                              color: '#ffffff',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                            }}
                          >
                            <Check size={12} />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Resilience / Placement Testing Control */}
                <div
                  style={{
                    marginTop: 'var(--space-4)',
                    padding: 'var(--space-3) var(--space-4)',
                    backgroundColor: 'var(--color-surface-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px dashed var(--color-border)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <HelpCircle size={16} color="var(--color-text-muted)" />
                    <div>
                      <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-secondary)' }}>
                        Payment Simulation Mode
                      </span>
                      <span style={{ display: 'block', fontSize: '11px', color: 'var(--color-text-muted)' }}>
                        Demonstrate backend fault handling & inventory rollback
                      </span>
                    </div>
                  </div>
                  <select
                    value={mockPaymentAction}
                    onChange={(e) => setMockPaymentAction(e.target.value)}
                    style={{
                      height: '32px',
                      fontSize: 'var(--font-size-xs)',
                      padding: '0 8px',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-surface)',
                      outline: 'none',
                    }}
                  >
                    <option value="SUCCESS">Success (Order Confirmed)</option>
                    <option value="FAIL_DECLINED">Simulate Card Declined (402)</option>
                    <option value="FAIL_INSUFFICIENT_FUNDS">Simulate Insufficient Funds</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Right Column: Order Summary & Place Order */}
            <div style={{ position: 'sticky', top: '100px' }}>
              <div className="card" style={{ padding: 'var(--space-6)' }}>
                <h3 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-4)', borderBottom: '1px solid var(--color-border)', paddingBottom: 'var(--space-3)' }}>
                  Order Summary ({items.length} {items.length === 1 ? 'item' : 'items'})
                </h3>

                {/* Items preview list */}
                <div style={{ maxHeight: '240px', overflowY: 'auto', marginBottom: 'var(--space-4)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
                  {items.map((item) => (
                    <div key={item.productId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div
                          style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: 'var(--radius-sm)',
                            backgroundColor: 'var(--color-surface-subtle)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                          }}
                        >
                          <ShoppingBag size={18} color="var(--color-text-muted)" />
                        </div>
                        <div>
                          <div style={{ fontWeight: 'var(--font-weight-medium)', maxWidth: '170px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {item.product?.name || 'Product'}
                          </div>
                          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                            Qty: {item.quantity} × {formatCurrency(item.product?.price || 0)}
                          </div>
                        </div>
                      </div>
                      <div style={{ fontWeight: 'var(--font-weight-semibold)' }}>
                        {formatCurrency((item.product?.price || 0) * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: 'var(--space-3)', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                    <span>Items Subtotal</span>
                    <span>{formatCurrency(subtotal)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Truck size={14} /> Shipping
                    </span>
                    <span style={{ color: 'var(--color-accent)', fontWeight: 'var(--font-weight-semibold)' }}>FREE</span>
                  </div>
                  <div
                    style={{
                      borderTop: '1px solid var(--color-border)',
                      paddingTop: 'var(--space-3)',
                      marginTop: 'var(--space-2)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: 'var(--font-size-lg)',
                      fontWeight: 'var(--font-weight-bold)',
                      color: 'var(--color-primary)',
                    }}
                  >
                    <span>Total Amount</span>
                    <span>{formatCurrency(finalTotal)}</span>
                  </div>
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  isLoading={isProcessing}
                  disabled={isProcessing}
                  icon={Lock}
                  style={{ width: '100%', marginTop: 'var(--space-6)' }}
                >
                  {isProcessing ? 'Processing Order...' : `Place Order • ${formatCurrency(finalTotal)}`}
                </Button>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    fontSize: 'var(--font-size-xs)',
                    color: 'var(--color-text-muted)',
                    marginTop: 'var(--space-4)',
                  }}
                >
                  <ShieldCheck size={14} color="var(--color-accent)" /> 256-bit SSL encrypted secure checkout
                </div>
              </div>
            </div>
          </div>
        </form>
      </Container>
    </div>
  );
}
