import { Link } from 'react-router-dom';
import { FiShoppingCart } from 'react-icons/fi';
import './ProductCard.css';

export default function ProductCard({ product }) {
  return (
    <Link to={`/product/${product.id}`} className="product-card card">
      <div className="product-image-wrapper">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} className="product-image" />
        ) : (
          <div className="product-image-placeholder">
            <span>📦</span>
          </div>
        )}
        {product.sellerName && (
          <span className="product-seller-badge">{product.sellerName}</span>
        )}
      </div>
      <div className="product-info">
        <h3 className="product-name">{product.name}</h3>
        {product.categoryName && (
          <span className="product-category">{product.categoryName}</span>
        )}
        <div className="product-footer">
          <span className="product-price">
            {new Intl.NumberFormat('tr-TR', {
              style: 'currency',
              currency: 'TRY',
            }).format(product.price)}
          </span>
          <button
            className="btn btn-primary btn-sm product-cart-btn"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
          >
            <FiShoppingCart />
          </button>
        </div>
      </div>
    </Link>
  );
}
