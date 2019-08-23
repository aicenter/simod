sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih.log /home/fiedlda1/Amodsim/rci_launchers/ih.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-sw.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr-sh.sh
sbatch --mem=75 -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga.log /home/fiedlda1/Amodsim/rci_launchers/vga.sh
sbatch --mem=75 -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-sw.sh
sbatch --mem=75 -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim.sh
sbatch --mem=75 -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim-sw.sh