import setuptools
from setuptools import setup

setup(
    name='amodsim',
    version='1.0',
    description='Support scripts for Agentpolis MoD simulation',
    author='David Fiedler',
    author_email='david.fido.fiedler@gmail.com',
    license='MIT',
    packages=setuptools.find_packages(),
    install_requires=['typing', 'pandas', 'fconfig', 'setuptools', 'roadmaptools', 'numpy', 'tqdm', 'matplotlib'],
    python_requires='>=3',
    package_data={'amodsim.resources': ['*.cfg']}
)
