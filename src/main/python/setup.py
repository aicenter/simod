import setuptools
from setuptools import setup

setup(
	name='amodsim',
	version='1.0.0',
	description='service scripts for amodsim agentpolis simulation',
	author='David Fiedler',
	author_email='david.fido.fiedler@gmail.com',
	license='MIT',
	packages=setuptools.find_packages(),
	install_requires=['roadmaptools','fconfig','numpy','pandas','matplotlib', 'tqdm', 'typing'],
	python_requires='>=3'
)
