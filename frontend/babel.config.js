module.exports = {
  presets: ['@babel/preset-env', '@babel/preset-react'],
  env: {
    test: {
      presets: ['@babel/preset-env', '@babel/preset-react'],
    },
  },
  plugins: [
    'react-hot-loader/babel',
    '@babel/plugin-proposal-class-properties',
    '@babel/plugin-proposal-object-rest-spread',
  ],
};
